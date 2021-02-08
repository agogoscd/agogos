import json
import os
import sys


class Chdir(object):
    """ Context manager for changing the current working directory """

    def __init__(self, new_path):
        self.newPath = os.path.expanduser(new_path)
        self.savedPath = None

    def __enter__(self):
        self.savedPath = os.getcwd()
        os.chdir(self.newPath)

    def __exit__(self, etype, value, traceback):
        os.chdir(self.savedPath)


# Get passed parameters
data = sys.argv[1]
results_dir = sys.argv[2]

print("Writing data to 'data.json' file in the {} directory...".format(results_dir))

with Chdir(results_dir):
    with open("data.json", "w") as f:
        f.write(data)

print("Done!")
