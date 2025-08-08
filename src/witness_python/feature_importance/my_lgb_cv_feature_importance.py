import pandas as pd
import lightgbm as lgb
from sklearn.preprocessing import StandardScaler

# Define dataset paths
training_data_dir = '../commons-csv/csv_1_fixed/'
train_dataset_path = training_data_dir + 'feature_data_train24.txt'
validation_dataset_path = training_data_dir + 'feature_data_val24.txt'

# Identify categorical and numerical features
categorical_features = ['StatementType', 'ParentContextType', 'MutationOperator', 'StatementDiff', 'SkeletonModification',
                          'HasReturnOrThrow', 'DeclaredVariableType', 'VariableIsFinalNew', 'HasThrow']
numerical_features = ['HitsNumber', 'NestingLevel', 'OccurringCount', 'ConditionalBlockLOC', 'ConditionalBlockCount',
                      'LinesInMethod', 'SourceComplexity', 'Call', 'Callby',
                      'LinesInTestCase', 'AssertionNumber', 'TestComplexity']
target = 'Label'

# Load datasets for LightGBM using pandas, adjusting for files separated by ';'
train_df = pd.read_csv(train_dataset_path, delimiter=';')
validation_df = pd.read_csv(validation_dataset_path, delimiter=';')

output_validation_df = validation_df[['TestNo', 'MutantNo', 'Label']].copy()

# Drop 'TestNo' and 'MutantNo' from pandas DataFrames
train_df.drop(['TestNo', 'MutantNo'], axis=1, inplace=True)
validation_df.drop(['TestNo', 'MutantNo'], axis=1, inplace=True)

# Apply Z-score normalization to numerical features
scaler = StandardScaler()
train_df[numerical_features] = scaler.fit_transform(train_df[numerical_features])
validation_df[numerical_features] = scaler.transform(validation_df[numerical_features])

# Convert categorical features to 'category' dtype in the training DataFrame
for cat_feature in categorical_features:
    train_df[cat_feature] = train_df[cat_feature].astype('category')
    validation_df[cat_feature] = validation_df[cat_feature].astype('category')

# Separate features and target for LightGBM
X_train = train_df[categorical_features + numerical_features]
y_train = train_df[target]
X_validation = validation_df[categorical_features + numerical_features]
y_validation = validation_df[target]

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

# Extracting feature importance
feature_importances = lgb_model.feature_importance(importance_type='split')

# Extracting feature names
feature_names = lgb_model.feature_name()

# Creating a DataFrame for feature importances
feature_importance_df = pd.DataFrame({
    'variable': feature_names,
    'importance': feature_importances
})

# Sorting the DataFrame by importance in descending order
feature_importance_df = feature_importance_df.sort_values(by='importance', ascending=False)
print(feature_importance_df)
