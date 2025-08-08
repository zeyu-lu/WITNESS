import h2o
import pandas as pd
import numpy as np
import lightgbm as lgb
from h2o.estimators import H2ORandomForestEstimator
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import roc_curve, precision_recall_curve

# Initialize H2O
h2o.init(max_mem_size="4G")

# Data paths
train_paths = [
    # '../jackson-core/jacksoncore_25_fixed/many-to-one/train_mtd_df.txt',
    '../gson/gson_15_fixed/many-to-one/train_mtd_df.txt',
    '../commons-lang/lang_1_fixed/many-to-one/train_mtd_df.txt',
    '../commons-cli/cli_30_fixed/many-to-one/train_mtd_df.txt',
    '../commons-csv/csv_15_fixed/many-to-one/train_mtd_df.txt',
]
validation_paths = [
    # '../jackson-core/jacksoncore_25_fixed/many-to-one/validation_mtd_df.txt',
    '../gson/gson_15_fixed/many-to-one/validation_mtd_df.txt',
    '../commons-lang/lang_1_fixed/many-to-one/validation_mtd_df.txt',
    '../commons-cli/cli_30_fixed/many-to-one/validation_mtd_df.txt',
    '../commons-csv/csv_15_fixed/many-to-one/validation_mtd_df.txt',
]
test_data_dir = '../jackson-core/jacksoncore_25_fixed/'
test_path = test_data_dir + 'feature_data_mtd.txt'


# Function to load and combine CSV files for a given list of paths
def load_and_combine_csv(paths):
    dfs = [pd.read_csv(path, delimiter=';') for path in paths]
    combined_df = pd.concat(dfs, ignore_index=True)
    return combined_df


# Load datasets
train_df = load_and_combine_csv(train_paths)
validation_df = load_and_combine_csv(validation_paths)
test_df = pd.read_csv(test_path, delimiter=';')

# output_validation_df = validation_df[['TestNo', 'MutantNo', 'Label']].copy()
output_df = test_df[['TestNo', 'MutantNo', 'Label']].copy()

# Drop 'TestNo' and 'MutantNo' columns
columns_to_drop = ['TestNo', 'MutantNo']
train_df.drop(columns=columns_to_drop, inplace=True, errors='ignore')
validation_df.drop(columns=columns_to_drop, inplace=True, errors='ignore')
test_df.drop(columns=columns_to_drop, inplace=True, errors='ignore')

# Define categorical and numerical features
categorical_features = ['StatementType', 'ParentContextType', 'MutationOperator', 'StatementDiff', 'SkeletonModification',
                        'HasReturnOrThrow', 'DeclaredVariableType', 'VariableIsFinalNew', 'HasThrow']
numerical_features = ['HitsNumber', 'NestingLevel', 'OccurringCount', 'ConditionalBlockLOC', 'ConditionalBlockCount',
                      'LinesInMethod', 'SourceComplexity', 'Call', 'Callby',
                      'LinesInTestCase', 'AssertionNumber', 'TestComplexity']

target = 'Label'

# Preprocess for H2O: Convert dataframe to H2OFrame and categorical features to factors
h2o_train_df = h2o.H2OFrame(train_df)
h2o_validation_df = h2o.H2OFrame(validation_df)
h2o_test_df = h2o.H2OFrame(test_df)

# Normalize numerical features in H2O datasets
for feature in numerical_features:
    mean = h2o_train_df[feature].mean()[0]  # Extract the mean value
    sd = h2o_train_df[feature].sd()[0]      # Extract the standard deviation value
    h2o_train_df[feature] = (h2o_train_df[feature] - mean) / sd
    h2o_validation_df[feature] = (h2o_validation_df[feature] - mean) / sd
    h2o_test_df[feature] = (h2o_test_df[feature] - mean) / sd

# Convert categorical features to factors in H2O DataFrames
for feature in categorical_features:
    h2o_train_df[feature] = h2o_train_df[feature].asfactor()
    h2o_validation_df[feature] = h2o_validation_df[feature].asfactor()
    h2o_test_df[feature] = h2o_test_df[feature].asfactor()

# Define and train H2O Random Forest model
h2o_rf = H2ORandomForestEstimator(
    seed=42,
    balance_classes=True,    # Balance 0s and 1s if class imbalance exists
    mtries=-1,               # Auto-selects sqrt(number_of_features)
    binomial_double_trees=True  # Improves separation in binary classification
)

h2o_rf.train(x=numerical_features + categorical_features, y=target, training_frame=h2o_train_df, validation_frame=h2o_validation_df)

# Make predictions with H2O Random Forest model
# Convert the H2O test DataFrame to include only the features used by the model
h2o_validation_df_for_pred = h2o_validation_df.drop([target], axis=1)
h2o_predictions_validation = h2o_rf.predict(h2o_validation_df_for_pred)['predict']

h2o_test_df_for_pred = h2o_test_df.drop([target], axis=1)

h2o_predictions_test = h2o_rf.predict(h2o_test_df_for_pred)['predict']

# Convert H2O predictions to numpy array for easier manipulation
h2o_predictions_validation_np = h2o_predictions_validation.as_data_frame().values.flatten()
h2o_predictions_test_np = h2o_predictions_test.as_data_frame().values.flatten()

# Scale numerical features
scaler = StandardScaler()
train_df[numerical_features] = scaler.fit_transform(train_df[numerical_features])
validation_df[numerical_features] = scaler.transform(validation_df[numerical_features])
test_df[numerical_features] = scaler.transform(test_df[numerical_features])

# Preprocessing
# Convert categorical columns to 'category' dtype
for col in categorical_features:
    train_df[col] = train_df[col].astype('category')
    validation_df[col] = validation_df[col].astype('category')
    test_df[col] = test_df[col].astype('category')

# Prepare datasets for LightGBM
X_train = train_df[categorical_features + numerical_features]
y_train = train_df[target]
X_validation = validation_df[categorical_features + numerical_features]
y_validation = validation_df[target]
X_test = test_df[categorical_features + numerical_features]

# Create LightGBM datasets
lgb_train = lgb.Dataset(X_train, label=y_train, categorical_feature=categorical_features)
lgb_eval = lgb.Dataset(X_validation, label=y_validation, reference=lgb_train, categorical_feature=categorical_features)

# LightGBM parameters
params = {
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
    params,
    lgb_train,
    num_boost_round=10000,
    valid_sets=[lgb_eval],
    callbacks=callbacks
)

# Make predictions on the test set
y_pred_validation = lgb_model.predict(X_validation)

y_pred_test = lgb_model.predict(X_test)

# Average the predicted probabilities from both models
average_predictions_validation = (h2o_predictions_validation_np + y_pred_validation) / 2

# Predefined set of thresholds
thresholds = np.arange(0.05, 0.51, 0.05)

# Calculate Precision-Recall Curve and ROC Curve
precision, recall, pr_thresholds = precision_recall_curve(validation_df[target], average_predictions_validation)
fpr, tpr, roc_thresholds = roc_curve(validation_df[target], average_predictions_validation)

# Function to find the closest threshold in the curve to our predefined set
def find_closest_threshold(predefined_thresholds, curve_thresholds, metric_values):
    closest_thresholds = np.array([curve_thresholds[np.abs(curve_thresholds - t).argmin()] for t in predefined_thresholds])
    metric_for_thresholds = np.array([metric_values[np.abs(curve_thresholds - t).argmin()] for t in predefined_thresholds])
    return closest_thresholds, metric_for_thresholds


# Find closest thresholds and their corresponding F1 scores for the Precision-Recall Curve
pr_closest_thresholds, pr_metrics = find_closest_threshold(thresholds, pr_thresholds, 2*precision*recall / (precision + recall))

# Find closest thresholds and their corresponding TPR-FPR (J statistic) for the ROC Curve
roc_closest_thresholds, roc_metrics = find_closest_threshold(thresholds, roc_thresholds, tpr - fpr)

print("pr_metrics: ", pr_metrics)
pr_max_index = max(enumerate(pr_metrics), key=lambda x: x[1])[0]
pr_max_metric = pr_metrics[pr_max_index]
pr_corresponding_threshold = pr_closest_thresholds[pr_max_index]
print("pr_max_metric:", pr_max_metric)
print("pr_corresponding_threshold:", pr_corresponding_threshold)

print("roc_metrics: ", roc_metrics)
roc_max_index = max(enumerate(roc_metrics), key=lambda x: x[1])[0]
roc_max_metric = roc_metrics[roc_max_index]
roc_corresponding_threshold = roc_closest_thresholds[roc_max_index]
print("roc_max_metric:", roc_max_metric)
print("roc_corresponding_threshold:", roc_corresponding_threshold)


def normalize_list(lst):
    """Normalizes a list of numbers using Z-score normalization."""
    mean_val = sum(lst) / len(lst)
    std_dev = (sum((x - mean_val) ** 2 for x in lst) / len(lst)) ** 0.5
    if std_dev == 0:  # Prevent division by zero
        return [0] * len(lst)  # or return lst if you don't want to change values in this case
    return [(x - mean_val) / std_dev for x in lst]


normalized_pr_metrics = normalize_list(pr_metrics)
normalized_roc_metrics = normalize_list(roc_metrics)

# Select the best threshold: This example uses a simple strategy of maximizing the mean of F1 score and J statistic
combined_metric = [p + r for p, r in zip(normalized_pr_metrics, normalized_roc_metrics)]
optimal_threshold_index = np.argmax(combined_metric)
optimal_threshold = thresholds[optimal_threshold_index]
print("Chosen Optimal Threshold:", optimal_threshold)
