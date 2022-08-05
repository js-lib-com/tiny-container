package com.jslib.tiny.container.spi;

/**
 * Container services executed on relevant points into container code flow. Container implementation discovers these processors
 * on an early phase of is life then execute them when program flow reach those relevant points. Current implementation deals
 * with two kinds of flow processors: life cycle events and join points.
 * 
 * A life cycle event is a moment in the life of an entity, e.g. container start, managed class loaded. A join point is a
 * location in a program where the control flow can arrive via multiple paths: in our case container services and application
 * logic.
 * 
 * On a particular container flow point is possible to execute more that one processor. Since execution order matters every flow
 * processor should provide a priority.
 * 
 * @author Iulian Rotaru
 */
public interface IFlowProcessor extends IContainerService {

	/**
	 * Get flow processor priority inside its processors set.
	 * 
	 * @return processor priority.
	 */
	IPriority getPriority();

}
