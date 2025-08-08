import pandas as pd
import numpy as np


# Function to load, split, and combine CSV files
def load_and_split_csv(paths, seed=42):
    train_dfs = []
    val_dfs = []

    for path in paths:
        df = pd.read_csv(path, delimiter=';')
        unique_mutants = df['MutantNo'].unique()
        np.random.seed(seed)  # Set seed at the beginning of function execution
        np.random.shuffle(unique_mutants)  # The shuffling will be consistent due to the fixed seed

        split_index = int(len(unique_mutants) * 0.9)
        train_mutants = set(unique_mutants[:split_index])
        val_mutants = set(unique_mutants[split_index:])

        train_dfs.append(df[df['MutantNo'].isin(train_mutants)])
        val_dfs.append(df[df['MutantNo'].isin(val_mutants)])

    combined_train_df = pd.concat(train_dfs, ignore_index=True)
    combined_val_df = pd.concat(val_dfs, ignore_index=True)

    return combined_train_df, combined_val_df


if __name__ == '__main__':
    workdirs = []
    workdirs.append('../jfreechart/chart_1_fixed/')
    for workdir in workdirs:
        train_val_paths = [
            # workdir + 'feature_data_mtd.txt',
            workdir + 'feature_data.txt',
        ]

        # Load dataset
        train_df, validation_df = load_and_split_csv(train_val_paths)
        print("len(train_df), len(validation_df):", len(train_df), len(validation_df))

        # Define file paths
        # train_path = workdir + 'many-to-one/train_mtd_df.txt'
        # validation_path = workdir + 'many-to-one/validation_mtd_df.txt'
        train_path = workdir + 'many-to-one/train_df.txt'
        validation_path = workdir + 'many-to-one/validation_df.txt'

        # Save the DataFrames to txt files with column names
        train_df.to_csv(train_path, sep=';', index=False)
        validation_df.to_csv(validation_path, sep=';', index=False)