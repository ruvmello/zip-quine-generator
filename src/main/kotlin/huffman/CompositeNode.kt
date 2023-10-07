package huffman

data class CompositeNode(var left: Node, var right: Node, override var weight: Int = 0): Node
