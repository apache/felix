package org.apache.felix.dm.shell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.osgi.framework.BundleContext;

/**
 * This class provides DependencyManager commands for the Gogo shell.
 */
public class GogoDMCommand extends DMCommand
{
    public GogoDMCommand(BundleContext context)
    {
        super(context);
    }
    
    public void dmhelp() {
        System.out.println("list Dependency Manager component diagnostics. Usage: dm [nodeps] [notavail] [compact] [<bundleid> ...]");
    }
    
    public void dm(String[] args) {
        execute("dm", args);
    }
        
   private void execute(String line, String[] args) {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
       ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();
       PrintStream out = new PrintStream(bytes);
       PrintStream err = new PrintStream(errorBytes);
        
       for (int i = 0; i < args.length; i ++) {
           line += " " + args[i];
       }
        
       super.execute(line.toString(), out, err);
       if (bytes.size() > 0) {
           System.out.println(new String(bytes.toByteArray()));
       }
       if (errorBytes.size() > 0) {
           System.out.print("Error:\n");
           System.out.println(new String(errorBytes.toByteArray()));
       }
    }
}
