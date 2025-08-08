import understand
import argparse


def get_mccabe_complexity(db_path, class_name, method_name, parameter_types=None):
    # Open the Understand database
    db = understand.open(db_path)

    try:
        found = False
        # Iterate over all class entities
        for class_entity in db.ents("Class"):
            # Check if the entity is the specified class or a subclass of it
            if class_entity.longname() == class_name or class_name in class_entity.longname():
                print("Class or subclass found:", class_entity.longname())  # Debug: Confirm class or subclass is found
                found = True

                # For each class or subclass, iterate over its members
                for method in class_entity.ents("Define", "Method"):
                    if method.simplename() == method_name:
                        # Retrieve the parameters of the method (if the API allows this)
                        method_parameters = [param.type() for param in method.ents("Define", "Parameter")]

                        if parameter_types:
                            # Compare the method's parameters with the expected parameter types
                            if method_parameters == parameter_types:
                                print("Overloaded Method found:", method.longname())  # Debug: Confirm method is found
                                metrics = method.metric(['Cyclomatic'])
                                if metrics and 'Cyclomatic' in metrics:
                                    return metrics['Cyclomatic']
                        else:
                            # If parameter_types is not provided, return the first match
                            print("Method found:", method.longname())  # Debug: Confirm method is found
                            metrics = method.metric(['Cyclomatic'])
                            if metrics and 'Cyclomatic' in metrics:
                                return metrics['Cyclomatic']

        if not found:
            print("Class or its subclasses not found in the database.")
            return None
    finally:
        # Always close the database
        db.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Calculate McCabe Complexity of a method using SciTools Understand.")
    parser.add_argument("project_path", help="The path to the .udb project file.")
    parser.add_argument("class_name", help="The fully qualified class name.")
    parser.add_argument("method_name", help="The method name.")
    parser.add_argument("--parameter_types", required=False, help="The comma-separated parameter types (optional).")

    args = parser.parse_args()

    parameter_types = args.parameter_types.split(',') if args.parameter_types else None

    # Example usage
    complexity = get_mccabe_complexity(args.project_path, args.class_name, args.method_name, parameter_types)
    if complexity is not None:
        print(
            f"McCabe Complexity of method '{args.method_name}' with parameters {parameter_types} in class '{args.class_name}' is: {complexity}")
    else:
        print("Method or class not found.")
