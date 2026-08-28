package com.cax.select.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

// root 输出
@Command(
        name = "JM-Selector",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "接入AI的本子搜寻器",
        subcommands = {FilterCommand.class, DownloadCommand.class}
)
public class RootCommand implements Runnable {

    @Override
    public void run() {
        // 未指定子命令时，打印用法
        CommandLine.usage(this, System.out);
    }
}
