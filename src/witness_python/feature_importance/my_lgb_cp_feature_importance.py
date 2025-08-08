import pandas as pd
import lightgbm as lgb
from sklearn.preprocessing import StandardScaler

# Data paths
train_paths = [
    '../jackson-core/jacksoncore_25_fixed/many-to-one/train_df.txt',
    '../gson/gson_15_fixed/many-to-one/train_df.txt',
    '../commons-lang/lang_1_fixed/many-to-one/train_df.txt',
    '../commons-cli/cli_30_fixed/many-to-one/train_df.txt',
    # '../commons-csv/csv_15_fixed/many-to-one/train_df.txt',
]
validation_paths = [
    '../jackson-core/jacksoncore_25_fixed/many-to-one/validation_df.txt',
    '../gson/gson_15_fixed/many-to-one/validation_df.txt',
    '../commons-lang/lang_1_fixed/many-to-one/validation_df.txt',
    '../commons-cli/cli_30_fixed/many-to-one/validation_df.txt',
    # '../commons-csv/csv_15_fixed/many-to-one/validation_df.txt',
]


# Function to load and combine CSV files for a given list of paths
def load_and_combine_csv(paths):
    dfs = [pd.read_csv(path, delimiter=';') for path in paths]
    combined_df = pd.concat(dfs, ignore_index=True)
    return combined_df


# Load datasets
train_df = load_and_combine_csv(train_paths)
validation_df = load_and_combine_csv(validation_paths)

# Drop 'TestNo' and 'MutantNo' columns
columns_to_drop = ['TestNo', 'MutantNo']
train_df.drop(columns=columns_to_drop, inplace=True, errors='ignore')
validation_df.drop(columns=columns_to_drop, inplace=True, errors='ignore')

categorical_features = ['StatementType', 'ParentContextType', 'MutationOperator', 'StatementDiff', 'SkeletonModification',
                        'HasReturnOrThrow', 'DeclaredVariableType', 'VariableIsFinalNew', 'HasThrow']
numerical_features = ['HitsNumber', 'NestingLevel', 'OccurringCount', 'ConditionalBlockLOC', 'ConditionalBlockCount',
                      'LinesInMethod', 'SourceComplexity', 'Call', 'Callby',
                      'LinesInTestCase', 'AssertionNumber', 'TestComplexity']

target = 'Label'

# Scale numerical features
scaler = StandardScaler()
train_df[numerical_features] = scaler.fit_transform(train_df[numerical_features])
validation_df[numerical_features] = scaler.transform(validation_df[numerical_features])

# Preprocessing
# Convert categorical columns to 'category' dtype
for col in categorical_features:
    train_df[col] = train_df[col].astype('category')
    validation_df[col] = validation_df[col].astype('category')

# Prepare datasets for LightGBM
X_train = train_df[categorical_features + numerical_features]
y_train = train_df[target]
X_validation = validation_df[categorical_features + numerical_features]
y_validation = validation_df[target]

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
