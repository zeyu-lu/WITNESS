import csv
import json
import xml.etree.ElementTree as ET


def get_test_dict(workdir):
    file1 = open(workdir + "testMap.csv", "r+")
    Lines1 = file1.readlines()

    num1 = 0
    test_dict = {}
    for line1 in Lines1:
        if num1 > 0:
            arr = line1.split(",")
            testno = int(arr[0])
            testname = arr[1].strip()
            test_dict[testno] = testname
        num1 += 1
    return test_dict


def get_mutant_dict(workdir):
    file = open(workdir + "mutants.log", "r+")
    Lines = file.readlines()
    print("len(Lines):", len(Lines))

    mutant_dict = {}
    for line in Lines:
        arr = line.split(":")
        mutantno = int(arr[0].strip())

        ridx = 2
        while True:
            if arr[len(arr)-ridx].strip().isnumeric():
                linenum = int(arr[len(arr)-ridx].strip())
                break
            else:
                ridx += 1

        ridx += 1
        cls = arr[len(arr)-ridx].split("@")[0]

        mydict = {cls: linenum}
        mutant_dict[mutantno] = mydict

    return mutant_dict


def get_classes_instrument(xmlfile):
    tree = ET.parse(xmlfile)
    root = tree.getroot()

    classes_instrument = []
    for cls in root.findall('./packages/package/classes/class'):
        classes_instrument.append(cls.attrib['name'])

    return classes_instrument


def get_test_info(workdir, testno):

    test = test_dict.get(testno)
    classes_modified = get_classes_instrument(workdir + "coverage.xml")

    test_info = {}
    mydict = dict()

    for cls in classes_modified:
        cls = cls.strip()
        print("cls:", cls)

        test = test.strip()
        print("test:", test)
        covmtx = json.load(open(workdir + test + "_" + cls + ".txt"))

        # mydict = {cls: covmtx}
        mydict[cls] = covmtx

    test_info[testno] = mydict

    return test_info


if __name__ == "__main__":
    workdir = "../jfreechart/chart_25_fixed/"

    file1 = open(workdir + 'hitsMap.csv', 'a')
    file1.write("{},{},{}".format("TestNo", "MutantNo", "hitsNumber") + "\n")

    test_dict = get_test_dict(workdir)
    mutant_dict = get_mutant_dict(workdir)  # mutant_dict[mutantno] = [cls, linenum]  mutants.log MutantNo -> LineNumber

    # Open the CSV file
    with open(workdir + 'covMap.csv', newline='') as csvfile:
        reader = csv.DictReader(csvfile)

        # Iterate over each row in the CSV file
        for row in reader:
            # Extract 'TestNo' and 'MutantNo' and convert them to integers
            testno = int(row['TestNo'])
            mutantno = int(row['MutantNo'])

            # Now you can use mutantno and testno as integer values
            print(f"TestNo: {testno}, MutantNo: {mutantno}")

            test_info = get_test_info(workdir, testno)  # test_info[testno] = [cls, covdict]  TestNo -> [LineNumber, Hits]
            my_test_dict = test_info.get(testno)

            for cls in my_test_dict:
                print("cls:", cls)
                my_mut_dict = mutant_dict.get(mutantno)
                for cls2 in my_mut_dict:
                    print("cls2:", cls2)
                    if cls == cls2:
                        covdict = my_test_dict.get(cls)
                        for key in covdict:
                            print("key:", key)
                            if str(key) == str(my_mut_dict.get(cls2)):
                                if int(covdict[key]) > 0:
                                    file1.write("{},{},{}".format(testno, mutantno, covdict[key]) + "\n")
                                else:
                                    file1.write("{},{},{}".format(testno, mutantno, 0) + "\n")

    file1.close()
