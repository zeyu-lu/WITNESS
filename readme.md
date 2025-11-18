# WITNESS: WITNESS: Unlocking lightweight and practical fine-grained predictive mutation testing

This supplementary material includes the dataset and source code for the paper, *WITNESS: Unlocking lightweight and practical fine-grained predictive mutation testing*.

## Requirements

- **Java**: 21  
- **Python**: 3.12

## The `src` Directory

This directory contains the source code required to run the experiments described in the paper. It includes the Java and Python implementations located in the `witness_java` and `witness_python` directories, respectively.

### 1. `witness_java`

This directory contains the source code for collecting 19 static features. The entry classes are `WITNESSMutant.java` and `WITNESSTest.java`, which invoke other Java classes to perform the feature collection tasks. After running `WITNESSMutant.java` and `WITNESSTest.java`, the generated files contain 19 features.

We use ANTLR to parse Java source classes. Specifically, we adopt `antlr-4.11.1-complete.jar`, and versions higher than 4.11.1 may not correctly collect features with the source code in `witness_java`. The `myjava` subdirectory is automatically generated using ANTLR based on the grammar files `JavaLexer.g4` and `JavaParser.g4`.

### 2. `witness_python`

#### (1) `features`

This directory contains all other source code related to the features of WITNESS. The `WITNESSMutant.java` file in `witness_java` and the two Python scripts `my_understand_call_callby_cli.py` and `my_understand_mccabe_cli.py` are responsible for collecting the features **Call**, **Callby**, and **SourceComplexity** in WITNESS. Specifically, `my_understand_call_callby_cli.py` and `my_understand_mccabe_cli.py` are invoked by `WITNESSMutant.java`. We use **Understand 6.4** to collect these features.

- The `hits_number` subdirectory collects the dynamic feature **HitsNumber**.
- The `test_complexity` subdirectory collects the feature **TestComplexity**.
- The `data_split` subdirectory contains the source code to split the feature data based on mutants. Specifically, we divide the mutants into a training set, validation set, and test set. We then create mutant-test pairs by associating each mutant with the corresponding test cases that can reach the mutated statement. As a result, the feature data in the training, validation, and test sets do not intersect with each other in terms of mutants.

#### (2) `models`

This subdirectory contains the source code of WITNESS for training models and performing predictions on test sets under different scenarios. File names ending with `_all` are scripts for all mutant-test pairs, while file names that do not end with `_all` are scripts for inside-source-method mutant-test pairs.

#### (3) `mutant_killing_reasons`

This subdirectory contains the source code for calculating the predictive performance under different mutant killing reasons.

#### (4) `fewer_covered_mutants`

This subdirectory contains the source code for calculating the F1-score of prediction results for fewer-covered mutants.

#### (5) `feature_importance`

This subdirectory contains the source code for calculating the contribution of features used in WITNESS.

#### (6) `test_case_prioritization`

This subdirectory contains the source code for conducting mutation-based test case prioritization using the predicted kill matrix.

#### (7) `optimal_threshold`

This subdirectory contains the source code for determining the optimal threshold using the validation set.

## The `data` Directory

This directory contains the 31 project versions used in this paper, which span 6 different projects.

For each project version, we provide the feature files collected for WITNESS. The feature files are named with the prefix `feature`, and those with `mtd` in the name contain feature data collected exclusively from mutant-test pairs where mutations occur inside source methods.

Additionally, each project version includes the necessary files to run our source code, such as:

- `covMap.csv`
- `killMap.csv`
- `mutants.log`
- `myCovMap.csv`
- `myCovMapMtd.csv`
- `testMap.csv`
