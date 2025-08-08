import h2o
from h2o.estimators import H2ORandomForestEstimator

# Initialize H2O
h2o.init(max_mem_size="4G")

# Define dataset paths
training_data_dir = '../commons-csv/csv_1_fixed/'
train_dataset_path = training_data_dir + 'feature_data_train.txt'
validation_dataset_path = training_data_dir + 'feature_data_val.txt'

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

# Drop 'TestNo' and 'MutantNo' from datasets
train_h2o = train_h2o.drop(['TestNo', 'MutantNo'], axis=1)
validation_h2o = validation_h2o.drop(['TestNo', 'MutantNo'], axis=1)

# Normalize numerical features in H2O datasets
for feature in numerical_features:
    mean = train_h2o[feature].mean()[0]  # Extract the mean value
    sd = train_h2o[feature].sd()[0]      # Extract the standard deviation value
    train_h2o[feature] = (train_h2o[feature] - mean) / sd
    validation_h2o[feature] = (validation_h2o[feature] - mean) / sd

# Convert categorical features to factors in H2O
for feature in categorical_features:
    train_h2o[feature] = train_h2o[feature].asfactor()
    validation_h2o[feature] = validation_h2o[feature].asfactor()
    # test_h2o[feature] = test_h2o[feature].asfactor()

rf_model = H2ORandomForestEstimator(
    seed=42,
    balance_classes=True,    # Balance 0s and 1s if class imbalance exists
    mtries=-1,               # Auto-selects sqrt(number_of_features)
    binomial_double_trees=True  # Improves separation in binary classification
)
rf_model.train(x=numerical_features + categorical_features, y=target, training_frame=train_h2o, validation_frame=validation_h2o)

# Get feature importance
feature_importance = rf_model.varimp(use_pandas=True)

# Display feature importance
print(feature_importance)
