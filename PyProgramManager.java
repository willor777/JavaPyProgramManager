
import com.google.gson.Gson;

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


    /**<h6>Starts the .py program as sub process with out adding initial parameters</h6>*/
    public PyProgramManager(Path pyProgramPath){
        this.pathToPyProgram = pyProgramPath.toAbsolutePath().toString();
        this.startPyProgram();
    }


    /**<h6>Starts the .py program WITH initial parameters as a sub process</h6>*/
    public PyProgramManager(Path pyProgramPath, HashMap<String, String> initialParams){
        this.pathToPyProgram = pyProgramPath.toAbsolutePath().toString();

        // Loop through map and add to the param data
        for(String key: initialParams.keySet()){
            this.addParam(key,initialParams.get(key));
        }
        this.startPyProgram();
    }


    /**<h6>Used in class to set up .py program with params. Params sent in STRING in format... $key:value$$key:value$
     * </h6>*/
    private void addParam(String key, String val){
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
        // Send input to py
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();
        // Set up output reader
        PyOutputRetrieval pyOutReader = new PyOutputRetrieval(this.pyProgramProcess);
        return pyOutReader.getJsonOutputAsMap();
    }


    /**<h6>Sends input string to waiting .py program. Waits for .py to send first STRING output and returns it.</h6>*/
    public String executeCommandGetStringOutput(String inputCommand){
        // Send input in
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();

        // Return string out(First Line .py prints)
        PyOutputRetrieval pyOutputRetrieval = new PyOutputRetrieval(this.pyProgramProcess);
        return pyOutputRetrieval.getStringOutput();
    }


    /**<h6>Sends input string to waiting .py program. Waits for .py to send first STRING output and prints to console.
     * </h6>*/
    public void executeCommandDisplayOutput(String inputCommand){
        // Send input in
        this.javaToPyInput.println(inputCommand);
        this.javaToPyInput.flush();

        // Print the first Line .py prints
        PyOutputRetrieval pyOutputRetrieval = new PyOutputRetrieval(this.pyProgramProcess);
        pyOutputRetrieval.displayOutput();
    }


    /**<h4>Convenience Class offering simple conversions of py output.
     * </h4>*/
    private static class PyOutputRetrieval{
        BufferedReader pyOutReader;
        public PyOutputRetrieval(Process pyProcess){
            this.pyOutReader = new BufferedReader(new InputStreamReader(pyProcess.getInputStream()));
        }


        /**<h5>Looks for a string that contains both a left and right curlybrace. Converts to HashMap(String, String)
         * </h5>*/
        public Map<String,String> getJsonOutputAsMap(){
            try{
                String nextOutput = this.pyOutReader.readLine();
                if(nextOutput == null){
                    return null;
                }else if((nextOutput != null) && (nextOutput.contains("{")) && (nextOutput.contains("}"))){
                    Gson gsonHelper = new Gson();
                    Map<String,String> rData = gsonHelper.fromJson(nextOutput,Map.class);
                    return rData;
                }else{
                    System.out.println("PyOutputRetrieval recieved non-JSON output ->" + nextOutput);
                    System.out.println("Returning NULL");
                    return null;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }


        /**<h5>Returns the first STRING type output from the .py file</h5>*/
        public String getStringOutput(){
            try{
                String nextOutput = this.pyOutReader.readLine();
                if(nextOutput != null){
                    return nextOutput;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }


        /**<h5>Prints first output found to console</h5>*/
        public void displayOutput(){
            try{
                String nextOut = this.pyOutReader.readLine();
                if(nextOut != null){
                    System.out.println("--Py Output START--");
                    System.out.println(">>>" + nextOut);
                    System.out.println("--Py Output END--");
                    return;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        Scanner javaInput = new Scanner(System.in);

        Path pathToPy = Paths.get("py_alchemy","_TESTING.py");
        PyProgramManager pyProgramManager = new PyProgramManager(pathToPy);
        pyProgramManager.startPyProgram();
        int x = 0;
        while(x < 5){
            System.out.println("Next .py Command: ");
            String nextCommand = javaInput.nextLine();

            x ++;
        }
    }
}
