import h2o
import pandas as pd
import numpy as np
import lightgbm as lgb
from h2o.estimators import H2ORandomForestEstimator
from sklearn.preprocessing import StandardScaler
from mypmt.predictive_performance.my_calculate_and_display_metrics import calculate_and_display_metrics

# Initialize H2O
h2o.init(max_mem_size="4G")

# Data paths
train_paths = [
    # '../jackson-core/jacksoncore_25_fixed/many-to-one/train_df.txt',
    '../gson/gson_15_fixed/many-to-one/train_df.txt',
    '../commons-lang/lang_1_fixed/many-to-one/train_df.txt',
    '../commons-cli/cli_30_fixed/many-to-one/train_df.txt',
    '../commons-csv/csv_15_fixed/many-to-one/train_df.txt',
]
validation_paths = [
    # '../jackson-core/jacksoncore_25_fixed/many-to-one/validation_df.txt',
    '../gson/gson_15_fixed/many-to-one/validation_df.txt',
    '../commons-lang/lang_1_fixed/many-to-one/validation_df.txt',
    '../commons-cli/cli_30_fixed/many-to-one/validation_df.txt',
    '../commons-csv/csv_15_fixed/many-to-one/validation_df.txt',
]
test_data_dir = '../jackson-core/jacksoncore_25_fixed/'
test_path = test_data_dir + 'feature_data.txt'


# Function to load and combine CSV files for a given list of paths
def load_and_combine_csv(paths):
    dfs = [pd.read_csv(path, delimiter=';') for path in paths]
    combined_df = pd.concat(dfs, ignore_index=True)
    combined_df.to_csv("../combined_df.txt", sep=';', index=False)
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
average_predictions_test = (h2o_predictions_test_np + y_pred_test) / 2

optimal_threshold = 0.35

# Generate final binary predictions using the chosen optimal threshold
average_predictions_binary = (average_predictions_test >= optimal_threshold).astype(int)

output_df['predict'] = average_predictions_binary

calculate_and_display_metrics(output_df, "WITNESS Predictions")

# Example of saving the output to a CSV file
output_file_path = test_data_dir + 'witness_predictions.csv'
output_df.to_csv(output_file_path, index=False)

print(f"Predictions saved to {output_file_path}")

covmap_methods = pd.read_csv(test_data_dir + 'myCovMapMtd.csv')

# Perform an inner merge to find matching rows (inside_method_predictions)
inside_method_predictions = output_df.merge(covmap_methods, on=['MutantNo', 'TestNo'], how='inner')

# Find rows in csv1 not matching covmap_methods (outside_method_predictions)
outside_method_predictions = output_df.merge(covmap_methods, on=['MutantNo', 'TestNo'], how='left', indicator=True)
outside_method_predictions = outside_method_predictions[outside_method_predictions['_merge'] == 'left_only'].drop(columns=['_merge'])

# Calculate metrics for inside_method_predictions
calculate_and_display_metrics(inside_method_predictions, "Inside Method Predictions")

# Calculate metrics for outside_method_predictions
calculate_and_display_metrics(outside_method_predictions, "Outside Method Predictions")
