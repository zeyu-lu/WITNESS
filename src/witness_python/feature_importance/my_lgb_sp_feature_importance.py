import pandas as pd
import lightgbm as lgb
from sklearn.preprocessing import StandardScaler

# Load dataset
workdir = '../commons-csv/csv_1_fixed/'

# Preprocessing steps
categorical_columns = ['StatementType', 'ParentContextType', 'MutationOperator', 'StatementDiff', 'SkeletonModification',
                          'HasReturnOrThrow', 'DeclaredVariableType', 'VariableIsFinalNew', 'HasThrow']
numerical_columns = ['HitsNumber', 'NestingLevel', 'OccurringCount', 'ConditionalBlockLOC', 'ConditionalBlockCount',
                      'LinesInMethod', 'SourceComplexity', 'Call', 'Callby',
                      'LinesInTestCase', 'AssertionNumber', 'TestComplexity']

train_df = pd.read_csv(workdir + 'same-version/train_df.txt', delimiter=';')
valid_df = pd.read_csv(workdir + 'same-version/validation_df.txt', delimiter=';')

# features = [col for col in data.columns if col not in [target, 'TestNo', 'MutantNo']]
features = categorical_columns + numerical_columns

# Define target and features
target = 'Label'  # Update this to your actual target column name

# Apply Z-score normalization to numerical features
scaler = StandardScaler()
train_df[numerical_columns] = scaler.fit_transform(train_df[numerical_columns])
valid_df[numerical_columns] = scaler.transform(valid_df[numerical_columns])

# For the training, validation, and test sets, convert categorical columns to 'category' dtype
for col in categorical_columns:
    train_df[col] = train_df[col].astype('category')
    valid_df[col] = valid_df[col].astype('category')

# Separate features and target for LightGBM
X_train = train_df[features]
y_train = train_df[target]
X_valid = valid_df[features]
y_valid = valid_df[target]

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
lgb_model = lgb.train(
    params,
    train_data,
    num_boost_round=10000,
    valid_sets=[valid_data],
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
