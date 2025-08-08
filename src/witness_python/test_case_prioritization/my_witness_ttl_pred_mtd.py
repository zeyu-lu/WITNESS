import subprocess
import random


def major_kill_matrix(workdir):
    file = open(workdir + "myCovMapMtd.csv", "r+")
    Lines = file.readlines()

    testset = set()
    mutantset = set()
    num1 = 0
    for line in Lines:
        if num1 > 0:
            arr = line.split(",")
            testno = int(arr[1])
            mutantno = int(arr[0])
            testset.add(testno - 1)
            mutantset.add(mutantno - 1)
        num1 += 1

    file1 = open(workdir + "testMap.csv", "r+")
    Lines1 = file1.readlines()
    rows = len(Lines1) - 1

    file2 = open(workdir + "mutants.log", "r+")
    Lines2 = file2.readlines()
    cols = len(Lines2)

    killmatrix = [[0 for i in range(cols)] for j in range(rows)]

    file3 = open(workdir + "witness_predictions.csv", "r+")
    Lines3 = file3.readlines()

    num = 0
    for line3 in Lines3:
        if num > 0:
            arr = line3.split(",")
            r = int(arr[0])
            r -= 1
            c = int(arr[1])
            c -= 1
            if int(arr[3]) == 1:
                killmatrix[r][c] = 1
        num += 1

    testlist = list(testset)
    mutantlist = list(mutantset)

    return killmatrix, testlist, mutantlist


def mutation_total(workdir, killmatrix, testlist, mutantlist):
    mylist = []
    for testno in testlist:
        mylist.append(testno)

    testmap= {}
    for testno in mylist:
        mytestlist = []
        mytestlist.append(testno)
        mutsc = compute_mutation_score(killmatrix, mytestlist, mutantlist)
        testmap[testno] = mutsc

    sorted_testmap = sort_dict_by_values(testmap)

    apfd(workdir, sorted_testmap)


def sort_dict_by_values(d):
    # Convert dictionary to a list of tuples
    items = list(d.items())

    # Shuffle the list to randomize the order
    random.shuffle(items)

    # Sort the list based on values in descending order
    items.sort(key=lambda x: x[1], reverse=True)

    # Convert the sorted list back to a dictionary
    sorted_dict = dict(items)

    return sorted_dict


def compute_mutation_score(killmatrix, testlist, mutantlist):
    newmatrix = []
    for t in testlist:
        tlist = []
        for m in mutantlist:
            tlist.append(killmatrix[t][m])
        newmatrix.append(tlist)
    killed = 0
    for i in range(0, len(mutantlist)):
        x = 0
        for j in range(0, len(testlist)):
            x = x + newmatrix[j][i]
            if x == 1:
                killed = killed + 1
                break
    return float(killed) / len(mutantlist) * 100


def get_test_map(workdir):
    file1 = open(workdir + "testMap.csv", "r+")
    Lines1 = file1.readlines()

    num1 = 0
    test_dict = {}
    for line1 in Lines1:
        if num1 > 0:
            arr = line1.split(",")
            testno = int(arr[0])
            testno -= 1
            testname = arr[1].strip()
            test_dict[testno] = testname
        num1 += 1
    return test_dict


def get_trigger_tests(workdir):
    proc = subprocess.Popen('cat defects4j.build.properties', shell=True, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, cwd=workdir)
    myread = proc.stdout.read()
    myread = str(myread)
    proc.communicate()
    splited = myread.split('d4j.tests.trigger=')
    d4j_tests_trigger = splited[-1][:-3]

    trigger_tests = []
    triggers = d4j_tests_trigger.split(",")
    for trigger in triggers:
        trigger = trigger.replace("::", "[")
        trigger = trigger + "]"
        trigger_tests.append(trigger)

    return trigger_tests


def apfd(workdir, sorted_testmap):
    test_dict = get_test_map(workdir)

    prioritized = []
    for testno in sorted_testmap:
        prioritized.append(test_dict[testno])

    trigger_tests = get_trigger_tests(workdir)

    n = len(prioritized)

    tf1 = n
    cnt = 1
    for test in prioritized:
        if test in trigger_tests:
            tf1 = cnt
            break
        cnt += 1

    res = 1.0 - float(tf1) / n + 1.0 / (2.0 * n)

    file1 = open(workdir + 'witness_ttl_pred.txt', 'a')
    output = "{};{};{};{};{}".format(prioritized, trigger_tests, res, tf1, n)
    file1.write(output + "\n")
    file1.close()


if __name__ == "__main__":
    myworkdir = "../jackson-core/jacksoncore_25_fixed/"

    killmatrix, testlist, mutantlist = major_kill_matrix(myworkdir)
    for i in range(0, 10):
        mutation_total(myworkdir, killmatrix, testlist, mutantlist)
