"""Compare all persisted view values; only voter/winner list order is unspecified."""

def canonical_history(value):
    if isinstance(value, dict):
        result = {}
        for field, nested in value.items():
            normalized = canonical_history(nested)
            if field in ("votes", "winners") and isinstance(normalized, list):
                normalized = sorted(normalized, key=lambda person: person["id"])
            result[field] = normalized
        return result
    if isinstance(value, list):
        return [canonical_history(item) for item in value]
    return value


def first_difference(before, after, path="$"):
    if type(before) is not type(after):
        return path
    if isinstance(before, dict):
        if before.keys() != after.keys():
            return path + ".fields"
        for field in before:
            difference = first_difference(before[field], after[field], path + "." + field)
            if difference:
                return difference
    elif isinstance(before, list):
        if len(before) != len(after):
            return path + ".length"
        for index, (left, right) in enumerate(zip(before, after)):
            difference = first_difference(left, right, f"{path}[{index}]")
            if difference:
                return difference
    elif before != after:
        return path
    return None


def assert_history_unchanged(before, after):
    difference = first_difference(canonical_history(before), canonical_history(after))
    if difference:
        # Report structural location only, never account data, cards or token values.
        raise AssertionError("Persisted game history changed at " + difference)
