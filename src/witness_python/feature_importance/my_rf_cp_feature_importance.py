import h2o
import pandas as pd
from h2o.estimators import H2ORandomForestEstimator

# Initialize H2O
h2o.init(max_mem_size="4G")

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
    combined_df.to_csv("../combined_df.txt", sep=';', index=False)
    return combined_df


# Load datasets
train_df = load_and_combine_csv(train_paths)
validation_df = load_and_combine_csv(validation_paths)

# Drop 'TestNo' and 'MutantNo' columns
columns_to_drop = ['TestNo', 'MutantNo']
train_df.drop(columns=columns_to_drop, inplace=True, errors='ignore')
validation_df.drop(columns=columns_to_drop, inplace=True, errors='ignore')

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

# Normalize numerical features in H2O datasets
for feature in numerical_features:
    mean = h2o_train_df[feature].mean()[0]  # Extract the mean value
    sd = h2o_train_df[feature].sd()[0]      # Extract the standard deviation value
    h2o_train_df[feature] = (h2o_train_df[feature] - mean) / sd
    h2o_validation_df[feature] = (h2o_validation_df[feature] - mean) / sd
    # h2o_test_df[feature] = (h2o_test_df[feature] - mean) / sd

# Convert categorical features to factors in H2O DataFrames
for feature in categorical_features:
    h2o_train_df[feature] = h2o_train_df[feature].asfactor()
    h2o_validation_df[feature] = h2o_validation_df[feature].asfactor()
    # h2o_test_df[feature] = h2o_test_df[feature].asfactor()

h2o_rf = H2ORandomForestEstimator(
    seed=42,
    balance_classes=True,    # Balance 0s and 1s if class imbalance exists
    mtries=-1,               # Auto-selects sqrt(number_of_features)
    binomial_double_trees=True  # Improves separation in binary classification
)
h2o_rf.train(x=numerical_features + categorical_features, y=target, training_frame=h2o_train_df, validation_frame=h2o_validation_df)

# Get feature importance
feature_importance = h2o_rf.varimp(use_pandas=True)

# Display feature importance
print(feature_importance)
