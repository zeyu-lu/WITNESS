import h2o
import pandas as pd
import lightgbm as lgb
from h2o.estimators import H2ORandomForestEstimator
from sklearn.preprocessing import StandardScaler
from mypmt.predictive_performance.my_calculate_and_display_metrics import calculate_and_display_metrics

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

# features = [col for col in data.columns if col not in [target, 'TestNo', 'MutantNo']]
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
print("rf_predictions_test:", rf_predictions_test)

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

output_df = test_df[['TestNo', 'MutantNo', 'Label']].copy()  #

combined_predictions_test = (rf_predictions_test['predict'] + lgb_predictions_test) / 2

# Evaluate performance
y_validation = valid_df[target].values
y_test = test_df[target].values  # Actual labels

optimal_threshold = 0.35

# Generate final binary predictions using the chosen optimal threshold
final_predictions = (combined_predictions_test >= optimal_threshold).astype(int)

# Adding a new column named 'PredictedClass'
output_df['predict'] = final_predictions
calculate_and_display_metrics(output_df, "WITNESS Predictions")

# # Example of saving the output to a CSV file
output_file_path = workdir + 'witness_predictions.csv'
output_df.to_csv(output_file_path, index=False)

covmap_methods = pd.read_csv(workdir + 'myCovMapMtd.csv')

# Perform an inner merge to find matching rows (inside_method_predictions)
inside_method_predictions = output_df.merge(covmap_methods, on=['MutantNo', 'TestNo'], how='inner')

# Find rows in output_df not matching covmap_methods (outside_method_predictions)
outside_method_predictions = output_df.merge(covmap_methods, on=['MutantNo', 'TestNo'], how='left', indicator=True)
outside_method_predictions = outside_method_predictions[outside_method_predictions['_merge'] == 'left_only'].drop(
    columns=['_merge'])

# Calculate metrics for inside_method_predictions
calculate_and_display_metrics(inside_method_predictions, "Inside Method Predictions")

# Calculate metrics for outside_method_predictions
calculate_and_display_metrics(outside_method_predictions, "Outside Method Predictions")

print("\nData extraction and evaluation complete.")

