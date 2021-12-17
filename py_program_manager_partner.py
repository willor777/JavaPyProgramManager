import sys
import json


class PyProgramManagerPartner:

    def __init__(self):
        self.initial_params = dict()

        self.exit_commands = [s.upper() for s in "q,quit,exit,leave,stop".split()]

    @classmethod
    def get_params_from_java(cls):
        """Pulls params from sys.argv and puts them into a dict and returns or returns None"""
        base = sys.argv[-1]
        if "$$" in base:
            base = base.split("$$")
            prms = dict()
            for kv_pair in base:
                k, v = base.split(":")
                prms[k] = v
            return prms

    @classmethod
    def send_data_out_as_json(cls, dataset_for_java: dict):
      """Convert all values of a dict to String, Then prints to java in JSON format"""
        # Convert all values to String to make it easier for java
        clean_data = dict()
        for k in dataset_for_java.keys():
            clean_data[str(k)] = str(dataset_for_java.get(k))
        print(clean_data)

    def run(self):
      """Main method of the class. Will attempt to set up params, Then begin infinite loop waiting for java commands. 
      If an exit command comes from java, Will shut down"""
        # Attempt to pull params from sys.argv[-1]. If none found 'self.initial_params' will = None
        self.initial_params = self.get_params_from_java()
        while True:
            java_command = input()

            if java_command.upper() in self.exit_commands:
                exit(1)

            # Do stuff here


if __name__ == '__main__':
    PyProgramManagerPartner().run()
