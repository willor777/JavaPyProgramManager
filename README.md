# JavaPyProgramRunner
A Java class that runs .py Programs with communication between the two. Java -> Py -> Java.

USE CASE.
-- Made this so that i can use my Python programs as sub-process in a Java application. Intended to be used with a py file that is constantly ready to recieve input() and execute input command.

Instructions....
(Set up)
1. Construct the PyProgramManager() class.
  - Can construct with just a Path to py file
  - Can also add key-value pairs to send to .py file
  - NOTE: Py program is started as sub process during PyProgramManager() Construction.
  - NOTE: Params entered at time of construction are sent to .py in format "$$key:value$$key:value$$" (So in the .py file just do a --key_val_pairs_list = sys.argv[-1].split("$$")) and create a dict() from the pairs.

(Interacting with .py program)
2. Use PyProgramManager().executeCommand(String inputCommand) methods to tell .py program what to do.
  - There are 3 methods to executeCommand and get results. (.executeCommandGetMapOutput(), .executeCommandGetStringOutput(), .executeCommandDisplayOutput())
  - All methods follow the same flow pattern... 
      - String inputCommand is sent to .py file (py is waiting for input())
      - Py program does what it needs to do and prints output to console (I make .py print a json string and turn it into a Map java side)  
      - While Py program is running, Java is waiting for output, If your py program sends no output it will get hung up. You need to atleast send a "completed" msg.



NOTE - I have included a .py Class that can act as a starting point / better explantion










