package com.proxeus;

public class Main {
    public static void main(String[] args) throws Exception {
        Config config = Application.init();
        new SparkServer(config);
    }
}
