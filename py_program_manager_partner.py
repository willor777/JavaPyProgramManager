import sys
import json
import traceback


class JavaPyProgramManagerHelper:

    def __init__(self):
        
        # Attempt to read params from sys.argv
        self.initial_params = self.get_initial_params_from_sys()

        # List of acceptable 'exit' codes
        self._exit_commands = [s.upper() for s in "q,quit,exit,leave,stop,end,close,bye,n,shutdown,shut down"
            .split(",")]

        
    def check_for_exit_command(self, current_command):
        """ Should be called after recieving the CMD from Java. If exit cmd is found will close program """
        if "?" in current_command:
            current_command = current_command.split("?")[0]
        if current_command.upper() in self._exit_commands:
            print("EXIT command recieved. Closing .py Program")
            exit(1)

    @staticmethod
    def decode_cmd(cmd):
        """
        To use in conjunction with Java's PyProgramManager's 'buildParamsString(HashMap<String,String>)'.
            - Takes a cmd string and looks for a "?" followed by params.
                - cmd String example...
                    - 'doSomething?key:value$key:value$;
                - Returns a tuple. (javaCommand, paramsDictOrEmptyDict)
        """
        if "?" not in cmd:
            return cmd, dict()

        cmd = cmd.split("?")
        str_prms = cmd[1]
        cmd = cmd[0]
        str_prms = str_prms.split("$")
        params = dict()
        for p in str_prms:
            if len(p) < 1:
                continue
            k, v = p.split(":")
            params[k] = v

        return cmd, params

    @classmethod
    def get_initial_params_from_sys(cls) -> dict:
        """
        Pulls starting params from sys.argv and puts them into a dict and returns or returns None.
            - Initial params can be given in PyProgramManager's Construction"""
        base = sys.argv[-1]
        if "$$" in base:
            base = base.split("$$")
            prms = dict()
            for kv_pair in base:
                if len(kv_pair) < 1 or ":" not in kv_pair:
                    continue
                k, v = kv_pair.split(":")
                prms[k] = v
            return prms
        else:
            return dict()

    @classmethod
    def return_data_as_json(cls, dict_or_list_of_dicts):
        """Converts data into JSON format and prints to Console for Java to catch"""
        if isinstance(dict_or_list_of_dicts, dict):
            # Convert all values to String to make it easier for java
            clean_data = dict()
            for k in dict_or_list_of_dicts.keys():
                clean_data[str(k)] = str(dict_or_list_of_dicts.get(k))
            print(clean_data)

        elif isinstance(dict_or_list_of_dicts, list):
            for dataset in dict_or_list_of_dicts:
                # Convert all values to String to make it easier for java
                clean_data = dict()
                for k in dataset.keys():
                    clean_data[str(k)] = str(dataset.get(k))
                print(clean_data)

    @classmethod
    def return_error_traceback(cls, error_method_root):
        """Returns the Traceback info for Java to print. The new line escape sequence is replaced and then re-added on Java side"""
        tb = traceback.format_exc().replace("\n", " --NEWLINE-- ")
        print(f"ERROR  -- {error_method_root} -- TRACEBACK START -- " + tb)

    @classmethod
    def return_error_message(cls, msg):
        """Returns a personalized error msg for Java to print to console"""
        print("ERROR -- " + msg)
        
        
    # TODO This is an example of the run() method your .py program should implement
    def run(self):
        while True:
            try:
                
                raw_cmd = input()
                self.check_for_exit_command(raw_cmd)
                
                cmd, params = self.decode_cmd(raw_cmd)
                
                # TODO Do stuff here
                
            except Exception as e:
                self.return_error_traceback("Location -- JavaPyProgramManagerHelper().run() Outter Loop")
                
            finally:
                print("END")
            
            
            
            


if __name__ == '__main__':
    PyProgramManagerPartner().run()
