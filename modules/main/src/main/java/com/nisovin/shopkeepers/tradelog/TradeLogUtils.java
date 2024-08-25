package com.nisovin.shopkeepers.tradelog;

import java.util.Map;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.yaml.YamlUtils;

/**
 * Helpers related to the logging of trades.
 */
public class TradeLogUtils {

	// Note: We log the item metadata in Yaml format, since this is what Bukkit natively supports
	// for serializing and deserializing ItemStacks. This ensures that we are able to load the data
	// again and recreate the original ItemStack if we wish to.
	// An alternative would be to log it in Json format, which may have better library support
	// across languages. However:
	// - Yaml is a super set of Json, so it is not guaranteed to be able to represent all future
	// serialized items, because certain Yaml features (e.g. object aliases / references) are not
	// supported.
	// - Gson (the Json library included with the Minecraft server and Bukkit) will not properly
	// preserve certain data types by default (at least not if we don't provide detailed custom
	// deserializers for every type of data that we may want to deserialize, or a deserializer that
	// replicates Yaml's parsing of certain primitive types, which is actually not that easily
	// possible): For instance, if the numeric data type of a loaded Json number is unknown, Gson
	// loads it as a double by default (without there being an easy way to change that). But since
	// some parts of Bukkit's ItemStack deserialization have strict expectations regarding the type
	// of data to deserialize, the deserialization from Json may fail for this data.
	public static String getItemMetadata(UnmodifiableItemStack itemStack) {
		assert itemStack != null;
		// If the logging of item metadata is enabled (not checked here), we not only store the
		// item's ItemMeta (if it has any), but also its data version. We therefore serialize the
		// complete item stack here, but then remove the item's type and amount again, since these
		// properties are already getting stored separately.
		Map<String, Object> itemData = itemStack.serialize(); // Assert: Modifiable map.
		itemData.remove("type");
		itemData.remove("amount");
		// In order to ensure single-line CSV records, we format the Yaml compactly:
		String yaml = YamlUtils.toCompactYaml(itemData);
		return yaml;
	}

	private TradeLogUtils() {
	}
}
