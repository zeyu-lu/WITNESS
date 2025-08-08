import understand
import csv


def get_mccabe_complexity(db, test_class, test_method):
    # Find the test class entity
    class_ent = next((ent for ent in db.ents("Java Class") if ent.longname() == test_class), None)

    if not class_ent:
        print(f"Test class '{test_class}' not found.")
        return None

    # Find the specific test method in the class
    for func in class_ent.ents("Define", "Java Method"):
        if func.simplename() == test_method:
            # Get McCabe Complexity metric
            complexity = func.metric(["Cyclomatic"])
            return complexity.get("Cyclomatic", None)

    print(f"Test method '{test_method}' not found in class '{test_class}'.")
    return None


# Function to read 'testMap.csv' and create a map from TestNo to TestName
def create_test_map(file_path):
    test_map = {}
    with open(file_path, newline='') as csvfile:
        csv_reader = csv.reader(csvfile)
        next(csv_reader, None)  # Skip the header
        for row in csv_reader:
            test_no, test_name = row
            test_map[int(test_no)] = test_name
    return test_map


# Function to extract file path, class name, and method name from TestName
def extract_test_details(test_name):
    # Extracting the class name (last part of the file path)
    class_name = test_name.split('[')[0]

    # Extracting the method name (inside the brackets)
    method_name = test_name.split('[')[1].rstrip(']')

    return class_name, method_name


if __name__ == "__main__":
    workdir = "../jfreechart/chart_25_fixed/"
    udb_file = workdir + 'chart_25_fixed.udb'

    cov_map_file_path = workdir + 'covMap.csv'
    test_map_file_path = workdir + 'testMap.csv'
    test_map = create_test_map(test_map_file_path)
    db = understand.open(udb_file)  # Open the Understand database

    output_file_path = workdir + "test_complexity.csv"  # Define the output CSV file path

    with open(cov_map_file_path, newline='') as csvfile:
        csv_reader = csv.reader(csvfile)
        header = next(csv_reader, None)  # Skip the header

        output_data = [["TestNo", "MutantNo", "TestComplexity"]]  # Prepare new header

        for row in csv_reader:
            print("\nrow:", row)
            test_no = int(row[0])  # Get TestNo from covMap.csv
            mutant_no = int(row[1])  # Get MutantNo

            test_name = test_map.get(test_no)  # Get TestName based on TestNo
            test_class, test_method = extract_test_details(test_name)

            mccabe_complexity = get_mccabe_complexity(db, test_class, test_method)
            print("mccabe_complexity:", mccabe_complexity)

            # Append the data to the list
            output_data.append([test_no, mutant_no, mccabe_complexity])

    # Write the new CSV file
    with open(output_file_path, mode='w', newline='') as outfile:
        csv_writer = csv.writer(outfile)
        csv_writer.writerows(output_data)

    print("Output written to:", output_file_path)

    db.close()
