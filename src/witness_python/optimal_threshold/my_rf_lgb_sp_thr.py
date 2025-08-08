import h2o
import numpy as np
import pandas as pd
import lightgbm as lgb
from h2o.estimators import H2ORandomForestEstimator
from sklearn.metrics import roc_curve, precision_recall_curve
from sklearn.preprocessing import StandardScaler

# Initialize H2O
h2o.init(max_mem_size="4G")

# Load dataset
workdir = '../commons-csv/csv_1_fixed/'

# Preprocessing steps
categorical_columns = ['StatementType', 'ParentContextType', 'MutationOperator', 'StatementDiff', 'SkeletonModification',
                          'HasReturnOrThrow', 'DeclaredVariableType', 'VariableIsFinalNew', 'HasThrow']
numerical_columns = ['HitsNumber', 'NestingLevel', 'OccurringCount', 'ConditionalBlockLOC', 'ConditionalBlockCount',
                      'LinesInMethod', 'SourceComplexity', 'Call', 'Callby',
                      'LinesInTestCase', 'AssertionNumber', 'TestComplexity']

features = categorical_columns + numerical_columns

# Define target and features
target = 'Label'  # Update this to your actual target column name

train = h2o.import_file(workdir + 'same-version/train_df.txt', sep=';')
validation = h2o.import_file(workdir + 'same-version/validation_df.txt', sep=';')
test = h2o.import_file(workdir + 'same-version/test_df.txt', sep=';')

# Normalize numerical features in H2O datasets
for feature in numerical_columns:
    mean = train[feature].mean()[0]  # Extract the mean value
    sd = train[feature].sd()[0]      # Extract the standard deviation value
    train[feature] = (train[feature] - mean) / sd
    validation[feature] = (validation[feature] - mean) / sd
    test[feature] = (test[feature] - mean) / sd

# Convert categorical columns to factors
for col in categorical_columns:
    train[col] = train[col].asfactor()
    validation[col] = validation[col].asfactor()
    test[col] = test[col].asfactor()

# H2O Random Forest Model Training
rf_model = H2ORandomForestEstimator(
    seed=42,
    balance_classes=True,    # Balance 0s and 1s if class imbalance exists
    mtries=-1,               # Auto-selects sqrt(number_of_features)
    binomial_double_trees=True  # Improves separation in binary classification
)
rf_model.train(x=features, y=target, training_frame=train, validation_frame=validation)

# Check the categorical encoding used
print(rf_model.actual_params["categorical_encoding"])

# Make predictions with RF model
rf_predictions_validation = rf_model.predict(validation)['predict'].as_data_frame()

rf_predictions_test = rf_model.predict(test)['predict'].as_data_frame()

train_df = pd.read_csv(workdir + 'same-version/train_df.txt', delimiter=';')
valid_df = pd.read_csv(workdir + 'same-version/validation_df.txt', delimiter=';')
test_df = pd.read_csv(workdir + 'same-version/test_df.txt', delimiter=';')

# Apply Z-score normalization to numerical features
scaler = StandardScaler()
train_df[numerical_columns] = scaler.fit_transform(train_df[numerical_columns])
valid_df[numerical_columns] = scaler.transform(valid_df[numerical_columns])
test_df[numerical_columns] = scaler.transform(test_df[numerical_columns])

# For the training, validation, and test sets, convert categorical columns to 'category' dtype
for col in categorical_columns:
    train_df[col] = train_df[col].astype('category')
    valid_df[col] = valid_df[col].astype('category')
    test_df[col] = test_df[col].astype('category')

# Separate features and target for LightGBM
X_train = train_df[features]
y_train = train_df[target]
X_valid = valid_df[features]
y_valid = valid_df[target]
X_test = test_df[features]
y_test = test_df[target]

# Assuming the rest of the DataFrame preparation has been done correctly
train_data = lgb.Dataset(X_train, label=y_train, categorical_feature=categorical_columns)
valid_data = lgb.Dataset(X_valid, label=y_valid, categorical_feature=categorical_columns)

# LightGBM Model Parameters
params = {
    'objective': 'binary',
    'metric': 'binary_logloss',
    'verbose': -1,
    'seed': 42
}

callbacks = [
    lgb.early_stopping(1000),  # Stop after 100 rounds without improvement
    lgb.log_evaluation(50)  # Log every 50 iterations
]
lgbm_model = lgb.train(
    params,
    train_data,
    num_boost_round=10000,
    valid_sets=[valid_data],
    callbacks=callbacks
)

# Make predictions with LightGBM model
lgb_predictions_validation = lgbm_model.predict(X_valid, num_iteration=lgbm_model.best_iteration)

lgb_predictions_test = lgbm_model.predict(X_test, num_iteration=lgbm_model.best_iteration)

# output_df = valid_df[['TestNo', 'MutantNo', 'Label']].copy()  #
output_df = test_df[['TestNo', 'MutantNo', 'Label']].copy()  #

combined_predictions_validation = (rf_predictions_validation['predict'] + lgb_predictions_validation) / 2

# Evaluate performance
y_validation = valid_df[target].values

# Predefined set of thresholds
thresholds = np.arange(0.05, 0.51, 0.05)

# Calculate Precision-Recall Curve and ROC Curve
precision, recall, pr_thresholds = precision_recall_curve(y_validation, combined_predictions_validation)
fpr, tpr, roc_thresholds = roc_curve(y_validation, combined_predictions_validation)

# Function to find the closest threshold in the curve to our predefined set
def find_closest_threshold(predefined_thresholds, curve_thresholds, metric_values):
    closest_thresholds = np.array([curve_thresholds[np.abs(curve_thresholds - t).argmin()] for t in predefined_thresholds])
    metric_for_thresholds = np.array([metric_values[np.abs(curve_thresholds - t).argmin()] for t in predefined_thresholds])
    return closest_thresholds, metric_for_thresholds


# Find closest thresholds and their corresponding F1 scores for the Precision-Recall Curve
pr_closest_thresholds, pr_metrics = find_closest_threshold(thresholds, pr_thresholds, 2*precision*recall / (precision + recall))

# Find closest thresholds and their corresponding TPR-FPR (J statistic) for the ROC Curve
roc_closest_thresholds, roc_metrics = find_closest_threshold(thresholds, roc_thresholds, tpr - fpr)


def normalize_list(lst):
    """Normalizes a list of numbers using Z-score normalization."""
    mean_val = sum(lst) / len(lst)
    std_dev = (sum((x - mean_val) ** 2 for x in lst) / len(lst)) ** 0.5
    if std_dev == 0:  # Prevent division by zero
        return [0] * len(lst)  # or return lst if you don't want to change values in this case
    return [(x - mean_val) / std_dev for x in lst]


print("pr_metrics:", pr_metrics)
pr_max_index = max(enumerate(pr_metrics), key=lambda x: x[1])[0]
pr_max_metric = pr_metrics[pr_max_index]
pr_corresponding_threshold = pr_closest_thresholds[pr_max_index]
print("pr_max_metric:", pr_max_metric)
print("pr_corresponding_threshold:", pr_corresponding_threshold)

print("roc_metrics:", roc_metrics)
roc_max_index = max(enumerate(roc_metrics), key=lambda x: x[1])[0]
roc_max_metric = roc_metrics[roc_max_index]
roc_corresponding_threshold = roc_closest_thresholds[roc_max_index]
print("roc_max_metric:", roc_max_metric)
print("roc_corresponding_threshold:", roc_corresponding_threshold)

# Normalize lists
normalized_pr_metrics = normalize_list(pr_metrics)
normalized_roc_metrics = normalize_list(roc_metrics)

combined_metric = [p + r for p, r in zip(normalized_pr_metrics, normalized_roc_metrics)]
print("combined_metric:", combined_metric)

# combined_metric = roc_metrics
optimal_threshold_index = np.argmax(combined_metric)
optimal_threshold = thresholds[optimal_threshold_index]
print("Chosen Optimal Threshold:", optimal_threshold)
