import h2o
import pandas as pd
import lightgbm as lgb
from h2o.estimators import H2ORandomForestEstimator
from sklearn.preprocessing import StandardScaler
from mypmt.predictive_performance.my_calculate_and_display_metrics import calculate_and_display_metrics

# Initialize H2O
h2o.init(max_mem_size="4G")

# Define dataset paths
training_data_dir = '../commons-csv/csv_15_fixed/'
train_dataset_path = training_data_dir + 'feature_data_mtd_train.txt'
validation_dataset_path = training_data_dir + 'feature_data_mtd_val.txt'

test_data_dir = '../jfreechart/chart_1_fixed/'
test_dataset_path = test_data_dir + 'feature_data_mtd.txt'

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

combined_predictions_test = (rf_predictions_test['predict'].values + lgb_pred_proba_test) / 2

optimal_threshold = 0.35

# Generate final binary predictions using the chosen optimal threshold
final_predictions = (combined_predictions_test >= optimal_threshold).astype(int)
output_test_df['predict'] = final_predictions
calculate_and_display_metrics(output_test_df, "WITNESS Predictions")

# # Example of saving the output to a CSV file
output_file_path = test_data_dir + 'witness_predictions.csv'
output_test_df.to_csv(output_file_path, index=False)

# Shut down H2O when everything is done
h2o.shutdown(prompt=False)
