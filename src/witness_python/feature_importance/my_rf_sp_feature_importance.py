import h2o
from h2o.estimators import H2ORandomForestEstimator

# Initialize H2O
h2o.init(max_mem_size="4G")

# Load dataset
workdir = '../commons-csv/csv_1_fixed/'

# Preprocessing steps
# numerical_columns = ['HitsNumber', 'NestingLevel', 'LinesInTestCase', 'LinesInMethod', 'Complexity', 'Call', 'Callby']
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

# Normalize numerical features in H2O datasets
for feature in numerical_columns:
    mean = train[feature].mean()[0]  # Extract the mean value
    sd = train[feature].sd()[0]      # Extract the standard deviation value
    train[feature] = (train[feature] - mean) / sd
    validation[feature] = (validation[feature] - mean) / sd

# Convert categorical columns to factors
for col in categorical_columns:
    train[col] = train[col].asfactor()
    validation[col] = validation[col].asfactor()
    # test[col] = test[col].asfactor()

# H2O Random Forest Model Training
# rf_model = H2ORandomForestEstimator(seed=42)
rf_model = H2ORandomForestEstimator(
    seed=42,
    balance_classes=True,    # Balance 0s and 1s if class imbalance exists
    mtries=-1,               # Auto-selects sqrt(number_of_features)
    binomial_double_trees=True  # Improves separation in binary classification
)
rf_model.train(x=features, y=target, training_frame=train, validation_frame=validation)

# Get feature importance
feature_importance = rf_model.varimp(use_pandas=True)

# Display feature importance
print(feature_importance)
