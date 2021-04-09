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
results_dir = sys.argv[1]

print("Writing data to 'data.json' file in the {} directory...".format(results_dir))

result = {"task-id": 33155498, "build-id": 1388246,
          "some-array": [1, 2, 3, 4, 5], "nested": {"thing": "one", "other": "two"}}

# Populate Tekton Task results.
# These are Tekton-native results which should be <4kB.
with Chdir(results_dir):
    with open("data", "w") as f:
        f.write(json.dumps(result))

print("Done!")
