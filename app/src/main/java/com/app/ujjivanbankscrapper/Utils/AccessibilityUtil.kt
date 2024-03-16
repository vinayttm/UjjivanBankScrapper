package com.app.ujjivanbankscrapper.Utils
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.google.gson.Gson
import java.util.*

class AccessibilityUtil {

    public  fun  findNodesByClassName(nodeInfo: AccessibilityNodeInfo?, className: String, nodes: MutableList<AccessibilityNodeInfo>) {
        if (nodeInfo == null) return

        if (nodeInfo.className.toString() == className) {
            nodes.add(nodeInfo)
        }

        for (i in 0 until nodeInfo.childCount) {
            val childNode = nodeInfo.getChild(i)
            findNodesByClassName(childNode, className, nodes)
        }
    }

    public  fun findNodeByPackageName(node: AccessibilityNodeInfo?, targetClassName: String): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.packageName.toString() == targetClassName) {
            return node
        }

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            val targetNode = findNodeByPackageName(childNode, targetClassName)
            if (targetNode != null) {
                return targetNode
            }
        }

        return null
    }

    public fun getTopMostParentNode(nodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var parentNode = nodeInfo
        var topMostParentNode: AccessibilityNodeInfo? = null

        while (parentNode != null) {
            topMostParentNode = parentNode
            parentNode = parentNode.parent
        }

        return topMostParentNode
    }

    public fun getAccessibilityNodeTreeAsXml(node: AccessibilityNodeInfo?): String {
        val xmlBuilder = StringBuilder()
        traverseAccessibilityNode(node, xmlBuilder, 0)
        return xmlBuilder.toString()
    }

    public  fun logLargeString(tag: String, message: String) {
        if (message.length > 500) {
            val chunkCount = message.length / 500
            for (i in 0..chunkCount) {
                val max = 500 * (i + 1)
                if (max >= message.length) {
                    Log.d(tag, "Chunk $i/$chunkCount: " + message.substring(500 * i))
                } else {
                    Log.d(tag, "Chunk $i/$chunkCount: " + message.substring(500 * i, max))
                }
            }
        } else {
            Log.d(tag, message)
        }
    }

    public  fun traverseAccessibilityNode(node: AccessibilityNodeInfo?, xmlBuilder: StringBuilder, depth: Int) {
        if (node == null) return

        for (i in 0 until depth) {
            xmlBuilder.append("\t")
        }

        xmlBuilder.append("<node")
            .append(" class=\"").append(node.className).append("\"")
            .append(" package=\"").append(node.packageName).append("\"")
            .append(" text=\"").append(node.text).append("\"")
            .append(" content-desc=\"").append(node.contentDescription).append("\"")

        val childCount = node.childCount
        if (childCount > 0) {
            xmlBuilder.append(">\n")
            for (i in 0 until childCount) {
                val childNode = node.getChild(i)
                traverseAccessibilityNode(childNode, xmlBuilder, depth + 1)
            }
            for (i in 0 until depth) {
                xmlBuilder.append("\t")
            }
            xmlBuilder.append("</node>\n")
        } else {
            xmlBuilder.append(" />\n")
        }

        node.recycle()
    }

    public fun findNodeByText(rootNode: AccessibilityNodeInfo?, text: String, deepSearch: Boolean, clickable: Boolean): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        for (i in 0 until rootNode.childCount) {
            val childNode = rootNode.getChild(i)
            if (childNode != null) {
                if (childNode.text != null && text == childNode.text.toString() && (!clickable || childNode.isClickable)) {
                    return childNode
                } else if (deepSearch) {
                    if (childNode.text != null && childNode.text.toString().contains(text) && (!clickable || childNode.isClickable)) {
                        return childNode
                    }
                }
                if (childNode.contentDescription != null && text == childNode.contentDescription.toString() && (!clickable || childNode.isClickable)) {
                    return childNode
                } else if (deepSearch) {
                    if (childNode.contentDescription != null && childNode.contentDescription.toString().contains(text) && (!clickable || childNode.isClickable)) {
                        return childNode
                    }
                }
                val foundNode = findNodeByText(childNode, text, deepSearch, clickable)
                childNode.recycle()
                if (foundNode != null) {
                    return foundNode
                }
            }
        }
        return null
    }

    public fun findNodeByClassName(node: AccessibilityNodeInfo?, targetClassName: String): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.className.toString() == targetClassName) {
            return node
        }

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            val targetNode = findNodeByClassName(childNode, targetClassName)
            if (targetNode != null) {
                return targetNode
            }
        }

        return null
    }

    public  fun findNodeByResourceId(node: AccessibilityNodeInfo?, targetResourceIdName: String): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.viewIdResourceName != null) {
            if (node.viewIdResourceName == targetResourceIdName || node.viewIdResourceName.contains(targetResourceIdName)) return node
        }

        for (i in 0 until node.childCount) {
            try {
                val childNode = node.getChild(i)
                val targetNode = findNodeByResourceId(childNode, targetResourceIdName)
                if (targetNode != null) {
                    return targetNode
                }
            } catch (ignored: Exception) {

            }

        }

        return null
    }

    public fun listAllTextsInActiveWindow(rootNode: AccessibilityNodeInfo?): List<String> {
        if (rootNode != null) {
            val allTexts: MutableList<String> = ArrayList()
            traverseNodesForText(rootNode, allTexts)
            rootNode.recycle()
            val gson = Gson()
            val json = gson.toJson(allTexts)
            Log.d("OUTPUT", json)
            return allTexts
        } else {
            Log.d("OUTPUT", "[]")
        }
        return ArrayList()
    }

    public fun listAllTextsInActiveWindow(rootNode: AccessibilityNodeInfo?, includeContentDesc: Boolean): List<String> {
        if (rootNode != null) {
            val allTexts: MutableList<String> = ArrayList()
            if (includeContentDesc) {
                traverseNodesForText(rootNode, allTexts)
            } else {
                traverseNodesForTextOnly(rootNode, allTexts)
            }
            rootNode.recycle()
            val gson = Gson()
            val json = gson.toJson(allTexts)
            Log.d("OUTPUT", json)
            return allTexts
        } else {
            Log.d("OUTPUT", "[]")
        }
        return ArrayList()
    }

    public fun traverseNodesForText(node: AccessibilityNodeInfo?, allTexts: MutableList<String>) {
        if (node == null) return
        val output = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        allTexts.add(output)

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            traverseNodesForText(childNode, allTexts)
        }
    }

    public fun traverseNodesForTextOnly(node: AccessibilityNodeInfo?, allTexts: MutableList<String>) {
        if (node == null) return
        val output = node.text?.toString() ?: ""
        allTexts.add(output)

        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            traverseNodesForTextOnly(childNode, allTexts)
        }
    }

    public fun findNodeByContentDescription(rootNode: AccessibilityNodeInfo?, targetContentDescription: String): AccessibilityNodeInfo? {
        if (rootNode == null) {
            return null
        }

        val childCount = rootNode.childCount

        for (i in 0 until childCount) {
            val childNode = rootNode.getChild(i)

            if (childNode != null) {
                val contentDescription = childNode.contentDescription

                if (contentDescription != null && targetContentDescription == contentDescription.toString()) {
                    return childNode
                }
                val foundNode = findNodeByContentDescription(childNode, targetContentDescription)

                if (foundNode != null) {
                    return foundNode
                }
                childNode.recycle()
            }
        }
        return null
    }

    public fun fixedPinedPosition(): List<Map<String, Any>> {
        val jsonArray: MutableList<Map<String, Any>> = ArrayList()
        val one: MutableMap<String, Any> = HashMap()
        one["x"] = 96
        one["y"] = 1083
        one["pin"] = "1"
        val two: MutableMap<String, Any> = HashMap()
        two["x"] = 276
        two["y"] = 1088
        two["pin"] = "2"
        val three: MutableMap<String, Any> = HashMap()
        three["x"] = 449
        three["y"] = 1087
        three["pin"] = "3"
        val four: MutableMap<String, Any> = HashMap()
        four["x"] = 104
        four["y"] = 1201
        four["pin"] = "4"
        val five: MutableMap<String, Any> = HashMap()
        five["x"] = 275
        five["y"] = 1206
        five["pin"] = "5"
        val six: MutableMap<String, Any> = HashMap()
        six["x"] = 445
        six["y"] = 1207
        six["pin"] = "6"
        val seven: MutableMap<String, Any> = HashMap()
        seven["x"] = 89
        seven["y"] = 1334
        seven["pin"] = "7"
        val eight: MutableMap<String, Any> = HashMap()
        eight["x"] = 259
        eight["y"] = 1311
        eight["pin"] = "8"
        val nine: MutableMap<String, Any> = HashMap()
        nine["x"] = 448
        nine["y"] = 1323
        nine["pin"] = "9"
        val zero: MutableMap<String, Any> = HashMap()
        zero["x"] = 268
        zero["y"] = 1439
        zero["pin"] = "0"
        jsonArray.add(one)
        jsonArray.add(two)
        jsonArray.add(three)
        jsonArray.add(four)
        jsonArray.add(five)
        jsonArray.add(six)
        jsonArray.add(seven)
        jsonArray.add(eight)
        jsonArray.add(nine)
        jsonArray.add(zero)
        return jsonArray
    }

   public fun findNodesByContentDescription(rootNode: AccessibilityNodeInfo?, targetContentDescription: String): List<AccessibilityNodeInfo> {
        val nodeList = mutableListOf<AccessibilityNodeInfo>()

        if (rootNode == null) {
            return nodeList
        }

        val childCount = rootNode.childCount

        for (i in 0 until childCount) {
            val childNode = rootNode.getChild(i)

            if (childNode != null) {
                val contentDescription = childNode.contentDescription

                if (contentDescription != null && targetContentDescription == contentDescription.toString()) {
                    nodeList.add(childNode)
                }

                nodeList.addAll(findNodesByContentDescription(childNode, targetContentDescription))
            }
        }

        return nodeList
    }

}
