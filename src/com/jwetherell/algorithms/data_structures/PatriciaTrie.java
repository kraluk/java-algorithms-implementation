package com.jwetherell.algorithms.data_structures;

import java.util.List;
import java.util.ArrayList;


/**
 * A Patricia trie is a space-optimized trie data structure where each non-terminating (black) node with only one child is merged 
 * with its child. The result is that every internal non-terminating (black) node has at least two children. Each terminating node 
 * (white) represents the end of a string.
 * 
 * http://en.wikipedia.org/wiki/Radix_tree
 * 
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class PatriciaTrie<C extends CharSequence> {

    protected Node<C> root = null;


    public PatriciaTrie() {
        root = new Node<C>(null);
    }
    
    public boolean add(C string) {
        Node<C> node = this.addNode(string);
        return (node!=null); 
    }
    
    @SuppressWarnings("unchecked")
    protected Node<C> addNode(C string) {
        int indexIntoParent = -1;
        int indexIntoString = -1;
        Node<C> node = root;
        for (int i=0; i<=string.length(); i++) {
            indexIntoString = i;
            indexIntoParent++;
            if (i==string.length()) break;

            char c = string.charAt(i);
            if (node.partOfThis(c, indexIntoParent)) {
                //Node has a char which is equal to char c at that index
                continue;
            } else if (node.string!=null && indexIntoParent < node.string.length()) {
                //string is equal to part of this Node's string
                break;
            }
            
            Node<C> child = node.getChildBeginningWithChar(c);
            if (child!=null) {
                //Found a child node starting with char c
                indexIntoParent = 0;
                node = child;
            } else {
                //Node doesn't have a child starting with char c
                break;
            }
        }

        Node<C> addedNode = null;
        if (node.string!=null && indexIntoParent<node.string.length()) {
            C parent = (C)node.string.subSequence(0, indexIntoParent);
            C refactor = (C)node.string.subSequence(indexIntoParent,node.string.length());
            node.string = parent;
            Node<C> newNode1 = new Node<C>(node, refactor, node.type);
            if (node.children.size()>0) {
                for (Node<C> c : node.children) {
                    c.parent = newNode1;
                    newNode1.children.add(c);
                }
            }
            node.children.clear();
            node.children.add(newNode1);
            if (indexIntoString==string.length()) node.type = Node.Type.white;
            else node.type = Node.Type.black;
            addedNode = newNode1;
            
            if (indexIntoString!=string.length()) {
                C newString = (C)string.subSequence(indexIntoString,string.length());
                Node<C> newNode2 = new Node<C>(node, newString, Node.Type.white);
                node.children.add(newNode2);
                addedNode = newNode2;
            }
        } else if (node.string!=null && string.length()==indexIntoString) {
            //Found a node who exactly matches a previous node
            
            //Already exists as a white node (leaf node)
            if (node.type == Node.Type.white) return null;
            
            //Was black (branching), now white (leaf)
            node.type = Node.Type.white;
            addedNode = node;
        } else if (node.string!=null) {
            //Adding a child
            C newString = (C)string.subSequence(indexIntoString,string.length());
            Node<C> newNode = new Node<C>(node, newString, Node.Type.white);
            node.children.add(newNode);
            addedNode = newNode;
        } else {
            //Add to root node
            Node<C> newNode = new Node<C>(node, string, Node.Type.white);
            node.children.add(newNode);
            addedNode = newNode;
        }

        return addedNode;
    }
    
    @SuppressWarnings("unchecked")
    public boolean remove(C string) {
        Node<C> node = getNode(string);
        if (node==null) return false;

        //No longer a white node (leaf)
        node.type = Node.Type.black;
        
        Node<C> parent = node.parent;
        if (node.children.size()==0) {
            //Remove the node if it has no children
            parent.children.remove(node);
        } else if (node.children.size()==1) {
            //Merge the node with it's child and add to node's parent
            parent.children.remove(node);
            
            Node<C> child = node.children.get(0);
            StringBuilder builder = new StringBuilder(node.string);
            builder.append(child.string);
            child.string = (C)builder.toString();
            child.parent = parent;
            
            parent.children.add(child);
        }

        //Walk up the tree and see if we can compact it
        while (parent!=null && parent.children.size()==1) {
            Node<C> child = parent.children.get(0);
            if (parent.type==Node.Type.black) {
                //Merge with parent
                StringBuilder builder = new StringBuilder(parent.string);
                builder.append(child.string);
                child.string = (C)builder.toString();
                child.parent = parent.parent;
            }
            parent.parent.children.remove(parent);
            parent.parent.children.add(child);
            
            parent = parent.parent;
        }

        return true;
    }
    
    public boolean contains(C string) {
        Node<C> node = getNode(string);
        return (node!=null && node.type==Node.Type.white);
    }

    protected Node<C> getNode(C string) { 
        int indexIntoParent = -1;
        Node<C> node = root;
        for (int i=0; i<string.length(); i++) {
            indexIntoParent++;

            char c = string.charAt(i);
            if (node.partOfThis(c, indexIntoParent)) {
                //Node has a char which is equal to char c at that index
                continue;
            } else if (node.string!=null && indexIntoParent < node.string.length()) {
                //string is equal to part of this Node's string
                break;
            }
            
            Node<C> child = node.getChildBeginningWithChar(c);
            if (child!=null) {
                //Found a child node starting with char c
                indexIntoParent = 0;
                node = child;
            } else {
                //Node doesn't have a child starting with char c
                break;
            }
        }
        
        if (node.string!=null && (indexIntoParent==(node.string.length()-1))) {
            //We ended our search at the last char in the node's string
            return node;
        }
        return null;
    }
    
    @Override
    public String toString() {
        return TreePrinter.getString(this);
    }


    protected static class Node<C extends CharSequence> implements Comparable<Node<C>> {
        
        protected enum Type { black, white };
        
        protected Node<C> parent = null;
        protected C string = null;
        protected Type type = Type.black;
        protected List<Node<C>> children = new ArrayList<Node<C>>();
        
        
        protected Node(Node<C> parent) { 
            this.parent = parent;
        }

        protected Node(Node<C> parent, C string) {
            this(parent);
            this.string = string;
        }

        protected Node(Node<C> parent, C string, Type type) {
            this(parent,string);
            this.type = type;
        }

        protected boolean partOfThis(char c, int idx) {
            //Search myself
            if (string!=null && idx<string.length() && string.charAt(idx)==c) return true;
            return false;
        }
        
        protected Node<C> getChildBeginningWithChar(char c) {
            //Search children
            Node<C> node = null;
            for (Node<C> child : this.children) {
                if (child.string.charAt(0) == c) return child;
            }
            return node;
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("string = ").append(string).append("\n");
            builder.append("type = ").append(type).append("\n");
            return builder.toString();
        }
        
        @Override
        public int compareTo(Node<C> node) {
            if (node==null) return -1;
            
            int length = string.length();
            if (node.string.length()<length) length = node.string.length();
            for (int i=0; i<length; i++) {
                Character a = string.charAt(i);
                Character b = node.string.charAt(i);
                int c = a.compareTo(b);
                if (c!=0) return c;
            }

            if (this.type.ordinal() < node.type.ordinal()) return -1;
            else if (this.type.ordinal() > node.type.ordinal()) return 1;
            
            if (this.children.size() < node.children.size()) return -1;
            else if (this.children.size() > node.children.size()) return 1;
            
            return 0;
        }
    }

    protected static class TreePrinter<C extends CharSequence> {

        protected static <C extends CharSequence> String getString(PatriciaTrie<C> tree) {
            return getString(tree.root, "", true);
        }

        protected static <C extends CharSequence> String getString(Node<C> node, String prefix, boolean isTail) {
            StringBuilder builder = new StringBuilder();
            builder.append(prefix + (isTail ? "└── " : "├── ") + ((node.string!=null)?node.string+" = "+node.type:"null")+"\n");
            if (node.children != null) {
                for (int i = 0; i < node.children.size() - 1; i++) {
                    builder.append(getString(node.children.get(i), prefix + (isTail ? "    " : "│   "), false));
                }
                if (node.children.size() >= 1) {
                    builder.append(getString(node.children.get(node.children.size() - 1), prefix + (isTail ?"    " : "│   "), true));
                }
            }
            return builder.toString();
        }
    }
}