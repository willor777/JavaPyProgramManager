package tools.pyProgramManager;

import com.google.gson.Gson;
import tools.DatesAndTimeHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**<h5>- Py program notes...</h5>
 * <h6>The .py program needs to have an infinite loop that waits for input each time.</h6>
 * <h6>The input should take a string(sent from java) and execute the command the string specifies</h6>
 * <h6>Then print the json return data back to be collected and Map'd by Java</h6>
 * <h6>It will then return to waiting for input to be sent(printed) to java</h6>


 * <h5>- Steps...</h5>
 * <h6>Construct PyProgramManager() class</h6>
 * <h6>Add initial params if any using .addParam(key,val)</h6>
 * <h6>Call .startPyProgram() --This starts the program as a sub Process</h6>
 * <h6>Use .sendInput(String input) to send string commands to the Py program</h6>
 * <h6>Use .getAllOutputData() to recieve all Output Strings(printed by py) as LinkedList(String)</h6>
 * <h6>Use .getLastOutputData() to recieve the last Output String(printed by py) by itself</h6>
 * */
public class PyProgramManager {

    private String pathToPyProgram;
    private ProcessBuilder processBuilder;
    private Process pyProgramProcess;
    private LinkedList<String> params = new LinkedList<>();

    private PrintStream javaToPyInput;
    private Thread pyOutputThread;


    /**<h6>Starts the .py program as sub process with out adding initial parameters.
     * Also creates a "Shut Down Hook" for when Java closes, The Py Program will be forcibly Closed</h6>*/
    public PyProgramManager(Path pyProgramPath){
        this.pathToPyProgram = pyProgramPath.toAbsolutePath().toString();
        this.startPyProgram();
        Runtime.getRuntime().addShutdownHook(new Thread(this.new PyShutdownAtExit(this)));
    }


    /**<h6>Starts the .py program WITH initial parameters as a sub process. Also adds a 'Shut Down Hook'
     * so that Java knows to close the py Program when it shuts down.</h6>*/
    public PyProgramManager(Path pyProgramPath, HashMap<String, String> initialParams){
        this.pathToPyProgram = pyProgramPath.toAbsolutePath().toString();

        // Loop through map and add to the param data
        for(String key: initialParams.keySet()){
            this.addInitialParamToSysParams(key,initialParams.get(key));
        }
        this.startPyProgram();
        Runtime.getRuntime().addShutdownHook(new Thread(this.new PyShutdownAtExit(this)));
    }


    public PyProgramManager(String pyProgramPath, HashMap<String,String> initialParams){
        this.pathToPyProgram = pyProgramPath;

        for(String key: initialParams.keySet()){
            this.addInitialParamToSysParams(key, initialParams.get(key));
        }
        this.startPyProgram();
        Runtime.getRuntime().addShutdownHook(new Thread(this.new PyShutdownAtExit(this)));
    }

    public PyProgramManager(String pyProgramPath){
        this.pathToPyProgram = pyProgramPath;

        this.startPyProgram();
        Runtime.getRuntime().addShutdownHook(new Thread(this.new PyShutdownAtExit(this)));
    }


    /**<h5>Forces sub-process .py program closed. Starts new one. For use if Java side application detects
     * a 'hung up' or 'stalled' .py program.</h5>*/
    public void resetPyProgram(){
        this.pyProgramProcess.destroyForcibly();
        this.startPyProgram();
    }


    /**<h6>Builds a param string of key val pairs in format '?key:val$$key:val$$'. meant to be added to end of cmd</h6>*/
    public String buildParamString(HashMap<String, String> keyValParams){
        String base = "?";
        for(String key: keyValParams.keySet()){
            String kvpair = key + ":" + keyValParams.get(key) + "$";
            base = base + kvpair;
        }
        return base;
    }


    /**<h6>Used to close the sub process safely by sending it the 'exit' command.
     * If py program is still running, Will '.destroyForcibly()'</h6>*/
    public void stopPyProcess(){
        this.executeCommandNoOutput("exit");
        try {
            Thread.sleep(500);
            if(this.pyProgramProcess.isAlive()){
                this.pyProgramProcess.destroyForcibly();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**<h6>Used in class to set up .py program with params. Params sent in STRING in format... $key:value$$key:value$
     * </h6>*/
    private void addInitialParamToSysParams(String key, String val){
        String joinedParam = "$" + key + ":" + val + "$";
        this.params.add(joinedParam);
    }


    /**<h6>Used in class to retrieve the .py parameter string when .py sub process is started</h6>*/
    private String getParamsForPy(){

        // Params will start end and be split with $$
        String formattedParams = "$";

        if (!(this.params.size() == 0)) {
            for (String s : params) {
                formattedParams = formattedParams + s;
            }
        } else {
            return "$$NoParams:None$$";
        }
        formattedParams = formattedParams + "$";
        return formattedParams;
    }


    /**<h6>Used in class to start the .py program and perform basic set up.</h6>
     * <h7>Error Stream is redirected to java console.</h7>
     * <h7> Java-to-Py input stream is also set up and can be called 'in class' using this.javaToPyInput</h7>*/
    private void startPyProgram(){
        this.processBuilder = new ProcessBuilder("python", this.pathToPyProgram, getParamsForPy());
        this.processBuilder.redirectErrorStream(true);
        try {
            // Start the program as a sub process
            this.pyProgramProcess = this.processBuilder.start();

            // Set up the print stream to print string commands into the py input
            this.javaToPyInput = new PrintStream(this.pyProgramProcess.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**<h6>Sends input string to a waiting .py program.</h6>
     * <h6>Waits for JSON output from .py program and returns it</h6>*/
    public Map<String,String> executeCommandGetMapOutput(String inputCommand){
        PyOutputRetrieval pyOutReader = new PyOutputRetrieval(this.pyProgramProcess);
        // Send input to py
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();
        // Set up output reader
        return pyOutReader.getJsonOutputAsMap();
    }


    /**<h6>Sends input string to a waiting .py program. Attempts to collect multiple JSON strings
     * until 'END' is printed from .py program</h6>*/
    public LinkedList<HashMap<String,String>> executeCommandGetMultipleMapOutput(String inputCommand){
        PyOutputRetrieval pyOut = new PyOutputRetrieval(this.pyProgramProcess);
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();
        LinkedList<HashMap<String, String>> out = pyOut.getMultipleMaps();
        return out;
    }

    /**<h6>Sends input string to waiting .py program. Waits for .py to send first STRING output and returns it.</h6>*/
    public String executeCommandGetStringOutput(String inputCommand){
        PyOutputRetrieval pyOutputRetrieval = new PyOutputRetrieval(this.pyProgramProcess);

        // Send input in
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();
        // Return string out(First Line .py prints)
        return pyOutputRetrieval.getStringOutput();
    }


    /**<h6>Sends input string to waiting .py program. Waits for .py to send first STRING output and prints to console.
     * </h6>*/
    public void executeCommandDisplayAllOutput(String inputCommand){
        PyOutputRetrieval pyOutputRetrieval = new PyOutputRetrieval(this.pyProgramProcess);
        // Send input in
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();
        // Print the first Line .py prints
        pyOutputRetrieval.displayAllOutput();
    }


    public void executeCommandNoOutput(String inputCommand){
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();
        return;
    }

    /**<h4>Convenience Class offering simple conversions of py output.
     * </h4>*/
    private static class PyOutputRetrieval{
        BufferedReader pyOutReader;
        public PyOutputRetrieval(Process pyProcess){
            this.pyOutReader = new BufferedReader(new InputStreamReader(pyProcess.getInputStream()));
        }


        private boolean checkForError(String lineOfPyOut){
            if(lineOfPyOut == null){
                System.out.println("Py Error >>> Null return from BufferedReader()");
                return true;
            }
            try{
                if(lineOfPyOut.length() < 5){
                    return false;
                }
                String errorCheck = lineOfPyOut.substring(0,5);
                if((errorCheck.toUpperCase().equals("ERROR"))||(errorCheck.contains("Traceback"))){
                    System.out.println(">>> [ CRITICAL ] [ " + DatesAndTimeHelper.getDateAndTimeString24H() + " ] " +
                            "\n>>> PyOutputReader Py Error Msg START....");
                    System.out.println(">>> ERROR MSG: " + lineOfPyOut.replaceAll("--NEWLINE--", "\n"));
                    while(true){
                        String nextLine = this.pyOutReader.readLine();
                        if (nextLine==null){
                            return true;
                        }else if(nextLine.toUpperCase().equals("END")){
                            System.out.println("\n>>> PyOutputReader Py Error Msg END...\n");
                            return true;
                        }
                        System.out.println(">>> " + nextLine.replaceAll("--NEWLINE--", "WORKING"));
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return false;
        }

        /**<h5>Looks for a string that contains both a left and right curlybrace. Converts to HashMap(String, String)
         * </h5>*/
        public Map<String,String> getJsonOutputAsMap(){
            try{
                while(true){
                    String nextOutput = this.pyOutReader.readLine();
                    boolean errorCheck = this.checkForError(nextOutput);
                    if((nextOutput == null)||(errorCheck == true)||(nextOutput.toUpperCase().equals("END"))){
                        return null;
                    }else if((nextOutput != null) && (nextOutput.contains("{")) && (nextOutput.contains("}"))){
                        Gson gsonHelper = new Gson();
                        Map<String,String> rData = gsonHelper.fromJson(nextOutput,Map.class);
                        return rData;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }


        /**<h5>Looks for multiple strings that start and end with curlybraces.
         * IMPORTANT Looks for 'END' from python program
         * to stop and end loop of json->map collecting</h5>*/
        public LinkedList<HashMap<String, String>> getMultipleMaps(){
            LinkedList<HashMap<String,String>> rData = new LinkedList<>();
            Gson gsonHelper = new Gson();
            try{
                // Loop until either null or "END" is recieved
                while(true){
                    String nextOutput = this.pyOutReader.readLine();
                    boolean errorCheck = this.checkForError(nextOutput);
                    if((nextOutput == null)||errorCheck||(nextOutput.toUpperCase().equals("END"))){
                        break;
                    }
                    try{
                        if((nextOutput.contains("{"))&&(nextOutput.contains("}"))){
                            rData.add(gsonHelper.fromJson(nextOutput,HashMap.class));
                        }
                    }catch (Exception e){
                        System.out.println("PyProgramManager.java -> getMultipleMaps() :" +
                                "String to Map conversion failed.");
                        continue;
                    }
                }
                return rData;
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }// TODO Change to Map

        /**<h5>Returns the first STRING type output from the .py file</h5>*/
        public String getStringOutput(){
            try{
                String nextOutput = this.pyOutReader.readLine();
                this.checkForError(nextOutput);
                if(nextOutput != null){
                    return nextOutput;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        /**<h5>Prints first output found to console</h5>*/
        public void displayAllOutput(){
            try{
                System.out.println("-- Py Out START --");
                while(true){
                    String nextOutput = this.pyOutReader.readLine();

                    if((nextOutput == null)||(nextOutput.toUpperCase().equals("END"))){
                        break;
                    }
                    System.out.println(">>> " + nextOutput.replaceAll("--NEWLINE--", "\n"));
                }
                System.out.println("-- Py Out END --");
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }


    /**<h5>Background thread that makes sure the .py Process is closed when the java program shuts down.
     * Uses 'destroyForcibly()' on the pyProcess.</h5>*/
    private class PyShutdownAtExit implements Runnable {
        private PyProgramManager pyProgramManager;

        public PyShutdownAtExit(PyProgramManager pyManager) {

            this.pyProgramManager = pyManager;
        }

        @Override
        public void run() {
            // Attempt to close process naturally with exit command
            this.pyProgramManager.stopPyProcess();
            // Check if still alive
            if (this.pyProgramManager.pyProgramProcess.isAlive()) {
                this.pyProgramManager.pyProgramProcess.destroy();
            }
            // Make sure it's dead with a 1s sleep and recheck then force destroy
            try {
                Thread.sleep(1000);
                if (this.pyProgramManager.pyProgramProcess.isAlive()) {
                    this.pyProgramManager.pyProgramProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Py Program Process Stopped");
        }
    }
}

