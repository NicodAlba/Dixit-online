from copy import deepcopy
import unittest
from history_check import assert_history_unchanged

class HistoryComparisonTest(unittest.TestCase):
    def fixture(self):
        return {"players": [{"id": 1, "points": 12}, {"id": 2, "points": 12}],
                "winners": [{"id": 1}, {"id": 2}],
                "turns": [{"id": 100, "playedCards": [
                    {"id": 10, "image": {"id": 3}, "user": {"id": 21}, "votes": [{"id": 1}, {"id": 2}]},
                    {"id": 11, "image": {"id": 4}, "user": {"id": 22}, "votes": []}]}]}

    def test_accepts_only_reordered_voters_and_winners(self):
        before = self.fixture(); after = deepcopy(before)
        after["winners"].reverse()
        after["turns"][0]["playedCards"][0]["votes"].reverse()
        assert_history_unchanged(before, after)

    def test_rejects_changed_points_authors_images_votes_and_fields(self):
        before = self.fixture()
        for mutation in (
            lambda value: value["players"][0].update(points=13),
            lambda value: value["turns"][0]["playedCards"][0]["user"].update(id=22),
            lambda value: value["turns"][0]["playedCards"][0]["image"].update(id=5),
            lambda value: value["turns"][0]["playedCards"][0]["votes"].pop(),
            lambda value: value["turns"][0]["playedCards"][0]["votes"].append({"id": 1}),
            lambda value: value["winners"].pop(),
            lambda value: value.update(unexpected="field"),
        ):
            after = deepcopy(before); mutation(after)
            with self.assertRaises(AssertionError): assert_history_unchanged(before, after)

    def test_retains_exact_anonymous_card_and_participant_order(self):
        before = self.fixture()
        for field in ("players", "table"):
            after = deepcopy(before)
            (after["players"] if field == "players" else after["turns"][0]["playedCards"]).reverse()
            with self.assertRaises(AssertionError): assert_history_unchanged(before, after)

if __name__ == "__main__":
    unittest.main()
