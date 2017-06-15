package com.zillion.classroom.highertolow.detection;

/**
 * @author Ganesan
 *
 */
public class ClassroomMovementDetectorTool {

	/**
	 * @param commandLineArgs
	 */
	public static void main(String[] commandLineArgs) {
		if(commandLineArgs.length >=2) {
			String path = commandLineArgs[0];
			String totalDaysBack = commandLineArgs[1];
			ClassroomMovementDetectorHelper.detectClasses(path,totalDaysBack);
		} 
		else 
		{
			System.out.println("Insufficient Parameters to detect the member movement between classrooms");
		}
	}
}