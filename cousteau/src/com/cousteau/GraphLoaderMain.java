package com.cousteau;

import java.util.Scanner;


public class GraphLoaderMain {
	
	public static void main(String ...strings) {
		
		final MsgTransport mta = new QueueMsgTransport();
		
		GraphManager gm = new GraphManager(mta);
		gm.start();
		
		IMAPMailGrabber grabber = new IMAPMailGrabber("orokhlin@gmail.com", "xap6kob80");
		grabber.setTransport(mta);
		grabber.setFetchCount(500);
		
		
		grabber.createSession();
		grabber.traverseFolder();
		grabber.disconnect();
				
        Scanner scan = new Scanner(System.in);
        
        System.out.println("Running... type 'quit' to exit: ");
        String line = null;
        while (!"quit".equalsIgnoreCase(line = scan.nextLine())) {
        	System.out.println("wrong command: " + line);
        }
        
        scan.close();
        gm.stop();
        
        try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        System.out.println("Stopped");
	}

}
