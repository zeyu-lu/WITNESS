import understand as und
import argparse


def count_method_relationships(db_path, class_name, method_name):
    db = und.open(db_path)
    calls_count = 0
    called_by_count = 0

    # Find the specified class
    target_class = db.lookup(class_name, "Class")

    if not target_class:
        print(f"Class '{class_name}' not found.")
        return

    # Check each class for the method
    for cls in target_class:
        print("cls:", cls)
        for method in cls.ents("Define", "Java Method"):
            if method.simplename() == method_name:
                # Count methods that this method calls
                calls_count = len(method.ents("Call", "Java Method"))
                # Count methods that call this method
                called_by_count = len(method.ents("Callby", "Java Method"))
                break

    db.close()
    return calls_count, called_by_count


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Count the number of methods a given method calls and is called by.')
    parser.add_argument('db_path', type=str, help='Path to Understand database file')
    parser.add_argument('class_name', type=str, help='Class name to search for the method')
    parser.add_argument('method_name', type=str, help='Method name to analyze')

    args = parser.parse_args()

    calls, called_by = count_method_relationships(args.db_path, args.class_name, args.method_name)

    print("Methods Calls '{}.{}':".format(args.class_name, args.method_name), calls)
    print("Methods Called By '{}.{}':".format(args.class_name, args.method_name), called_by)

