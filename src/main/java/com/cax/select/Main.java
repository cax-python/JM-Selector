package com.cax.select;

import com.cax.select.cli.RootCommand;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        String art = """
                  ____    _    __  __
                 / ___|  / \\   \\ \\/ /
                | |     / _ \\   \\  /\s
                | |___ / ___ \\  /  \\\s
                 \\____/_/   \\_\\/_/\\_\\
                
                """;
        System.out.println(art);
        int exitCode = new CommandLine(new RootCommand()).execute(args);
        System.exit(exitCode);
    }
}
