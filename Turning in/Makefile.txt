all: Body SpaceThread ParallelCollisions

Body: Body.java
	javac Body.java
	
SpaceThread: SpaceThread.java
	javac SpaceThread.java
	
ParallelCollisions: ParallelCollisions.java
	javac ParallelCollisions.java