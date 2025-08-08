import h2o
import pandas as pd
import numpy as np
import lightgbm as lgb
from h2o.estimators import H2ORandomForestEstimator
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import roc_curve, precision_recall_curve

# Initialize H2O
h2o.init(max_mem_size="4G")

# Define dataset paths
training_data_dir = '../commons-csv/csv_1_fixed/'
train_dataset_path = training_data_dir + 'feature_data_mtd_train24.txt'
validation_dataset_path = training_data_dir + 'feature_data_mtd_val24.txt'

test_data_dir = '../commons-csv/csv_5_fixed/'
test_dataset_path = test_data_dir + 'feature_data_mtd24.txt'

# Identify categorical and numerical features
categorical_features = ['StatementType', 'ParentContextType', 'MutationOperator', 'StatementDiff', 'SkeletonModification',
                          'HasReturnOrThrow', 'DeclaredVariableType', 'VariableIsFinalNew', 'HasThrow']
numerical_features = ['HitsNumber', 'NestingLevel', 'OccurringCount', 'ConditionalBlockLOC', 'ConditionalBlockCount',
                      'LinesInMethod', 'SourceComplexity', 'Call', 'Callby',
                      'LinesInTestCase', 'AssertionNumber', 'TestComplexity']
target = 'Label'

# Load training and test datasets for H2O
train_h2o = h2o.import_file(train_dataset_path, sep=";")
validation_h2o = h2o.import_file(validation_dataset_path, sep=";")
test_h2o = h2o.import_file(test_dataset_path, sep=";")

# Drop 'TestNo' and 'MutantNo' from datasets
train_h2o = train_h2o.drop(['TestNo', 'MutantNo'], axis=1)
validation_h2o = validation_h2o.drop(['TestNo', 'MutantNo'], axis=1)
test_h2o = test_h2o.drop(['TestNo', 'MutantNo'], axis=1)

# Normalize numerical features in H2O datasets
for feature in numerical_features:
    mean = train_h2o[feature].mean()[0]  # Extract the mean value
    sd = train_h2o[feature].sd()[0]      # Extract the standard deviation value
    train_h2o[feature] = (train_h2o[feature] - mean) / sd
    validation_h2o[feature] = (validation_h2o[feature] - mean) / sd
    test_h2o[feature] = (test_h2o[feature] - mean) / sd

# Convert categorical features to factors in H2O
for feature in categorical_features:
    train_h2o[feature] = train_h2o[feature].asfactor()
    validation_h2o[feature] = validation_h2o[feature].asfactor()
    test_h2o[feature] = test_h2o[feature].asfactor()

# Train the H2O Random Forest model
rf_model = H2ORandomForestEstimator(
    seed=42,
    balance_classes=True,    # Balance 0s and 1s if class imbalance exists
    mtries=-1,               # Auto-selects sqrt(number_of_features)
    binomial_double_trees=True  # Improves separation in binary classification
)
rf_model.train(x=numerical_features + categorical_features, y=target, training_frame=train_h2o, validation_frame=validation_h2o)

# Predict with the H2O Random Forest model
rf_predictions_train = rf_model.predict(train_h2o)['predict'].as_data_frame()
rf_predictions_validation = rf_model.predict(validation_h2o)['predict'].as_data_frame()

rf_predictions_test = rf_model.predict(test_h2o)['predict'].as_data_frame()

# Load datasets for LightGBM using pandas, adjusting for files separated by ';'
train_df = pd.read_csv(train_dataset_path, delimiter=';')
validation_df = pd.read_csv(validation_dataset_path, delimiter=';')
test_df = pd.read_csv(test_dataset_path, delimiter=';')

output_validation_df = validation_df[['TestNo', 'MutantNo', 'Label']].copy()
output_test_df = test_df[['TestNo', 'MutantNo', 'Label']].copy()  #

# Drop 'TestNo' and 'MutantNo' from pandas DataFrames
train_df.drop(['TestNo', 'MutantNo'], axis=1, inplace=True)
validation_df.drop(['TestNo', 'MutantNo'], axis=1, inplace=True)
test_df.drop(['TestNo', 'MutantNo'], axis=1, inplace=True)

# Apply Z-score normalization to numerical features
scaler = StandardScaler()
train_df[numerical_features] = scaler.fit_transform(train_df[numerical_features])
validation_df[numerical_features] = scaler.transform(validation_df[numerical_features])
test_df[numerical_features] = scaler.transform(test_df[numerical_features])

# Convert categorical features to 'category' dtype in the training DataFrame
for cat_feature in categorical_features:
    train_df[cat_feature] = train_df[cat_feature].astype('category')
    validation_df[cat_feature] = validation_df[cat_feature].astype('category')
    test_df[cat_feature] = test_df[cat_feature].astype('category')  # Also convert for test_df for prediction consistency

# Separate features and target for LightGBM
X_train = train_df[categorical_features + numerical_features]
y_train = train_df[target]
X_validation = validation_df[categorical_features + numerical_features]
y_validation = validation_df[target]
X_test = test_df[categorical_features + numerical_features]

# Create LightGBM dataset with categorical feature handling
train_set = lgb.Dataset(X_train, label=y_train, categorical_feature=categorical_features, free_raw_data=False)
validation_set = lgb.Dataset(X_validation, label=y_validation, categorical_feature=categorical_features, reference=train_set, free_raw_data=False)

# Define LightGBM parameters
lgb_params = {
    'objective': 'binary',  # Update this according to your task
    'metric': 'binary_logloss',
    'verbose': -1,
    'seed': 42
}

# Train the LightGBM model
callbacks = [
    lgb.early_stopping(1000),  # Stop after 100 rounds without improvement
    lgb.log_evaluation(50)  # Log every 50 iterations
]
lgb_model = lgb.train(
    lgb_params,
    train_set,
    num_boost_round=10000,
    valid_sets=[validation_set],
    callbacks=callbacks
)

# Predict with the LightGBM model
lgb_pred_proba_train = lgb_model.predict(X_train)
lgb_pred_proba_validation = lgb_model.predict(X_validation)

lgb_pred_proba_test = lgb_model.predict(X_test)

# Actual labels
y_test = test_df[target].values

# Predefined set of thresholds
thresholds = np.arange(0.05, 0.51, 0.05)

# Combine the predicted probabilities and generate final binary predictions
combined_predictions_validation = (rf_predictions_validation['predict'].values + lgb_pred_proba_validation) / 2

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

# combined_metric = pr_metrics + roc_metrics
combined_metric = [p + r for p, r in zip(normalized_pr_metrics, normalized_roc_metrics)]
print("combined_metric:", combined_metric)
optimal_threshold_index = np.argmax(combined_metric)
optimal_threshold = thresholds[optimal_threshold_index]
print("Chosen Optimal Threshold:", optimal_threshold)
