package js.tiny.container.rest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.util.Params;

public class PathTree<T> {
	private final Node root;

	public PathTree() {
		this.root = new Node();
	}

	public void put(List<String> path, T value) {
		Params.notNull(path, "Path");
		Params.GT(path.size(), 0, "Path size");

		getLeaf(root, path, 0).setValue(value);
	}

	private static Node getLeaf(Node node, List<String> path, int pathIndex) {
		String pathComponent = path.get(pathIndex);

		Node child = node.getChild(pathComponent);
		if (child == null) {
			if (pathComponent.charAt(0) == '{') {
				child = new Variable();
				node.putVariable((Variable) child);
			} else {
				child = new Node();
				node.putChild(pathComponent, child);
			}
		}

		if (pathIndex == path.size() - 1) {
			return child;
		}

		return getLeaf(child, path, pathIndex + 1);
	}

	public Item<T> get(List<String> path) {
		Params.notNull(path, "Path");
		Params.GT(path.size(), 0, "Path size");

		List<String> variables = new ArrayList<>();
		@SuppressWarnings("unchecked")
		T value = (T) search(root, path, 0, variables);
		return new Item<>(value, variables);
	}

	private static Object search(Node node, List<String> path, int pathIndex, List<String> variables) {
		String pathComponent = path.get(pathIndex);

		Node child = node.getChild(pathComponent);
		if(child == null) {
			return null;
		}
		if (child instanceof Variable) {
			variables.add(pathComponent);
		}

		if (pathIndex == path.size() - 1) {
			return child.getValue();
		}

		return search((Node) child, path, pathIndex + 1, variables);
	}

	// --------------------------------------------------------------------------------------------

	public static class Item<T> {
		private static final Converter converter = ConverterRegistry.getConverter();
		
		private final T value;
		private final List<String> variables;

		public Item(T value) {
			this.value = value;
			this.variables = Collections.emptyList();
		}

		public Item(T value, List<String> variables) {
			this.value = value;
			this.variables = variables;
		}

		public T getValue() {
			return value;
		}

		public List<String> getVariables() {
			return variables;
		}

		public boolean hasVariables() {
			return !variables.isEmpty();
		}

		public Object getVariableValue(int index, Type formalParameters) {
			return converter.asObject(variables.get(index), (Class<?>) formalParameters);
		}
	}

	private static class Node {
		private final HashMap<String, Node> children = new HashMap<>();
		private Variable variable;
		private Object value;

		public Node getChild(String name) {
			Node child = children.get(name);
			return child != null ? child : variable;
		}

		public void putChild(String name, Node child) {
			children.put(name, child);
		}

		public void putVariable(Variable variable) {
			this.variable = variable;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public Object getValue() {
			return value;
		}
	}

	private static class Variable extends Node {
	}
}
