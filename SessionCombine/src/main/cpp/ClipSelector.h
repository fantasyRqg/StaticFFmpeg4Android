//
// Created by Fan Zhenya on 19/04/2017.
//
#ifndef FINDBESTCLIPFROMRANK_CLIPSELECTOR_H
#define FINDBESTCLIPFROMRANK_CLIPSELECTOR_H

#include <vector>
#include <random>

using std::vector;
using std::pair;

class ClipSelector {
public:
    ClipSelector(vector<double> rank, int rank_sample_rate) {
        dp_.reserve(rank.size() + 1);
        dp_[0] = 0;
        for (int i = 0; i < rank.size(); ++i) {
            dp_[i + 1] = dp_[i] + rank[i];
        }
        rank_sample_rate_ = rank_sample_rate;
        rank_ = rank;
    }

    /*
     * Select up to N (up to @max_n_clips) clips from the rank, maximizing the the rank contained in each clip.
     * At the same time, he selected clips should satisfy these constraints:
     *   1. length (number of frames) is a random variable within region: (@min_clip_len, @max_clip_len)
     *   2. margin (number of frames) between two adjacent clips > @margin_between_clips
     *
     * Return: a list of clips denoted by std::pair<start_frame_number, end_frame_number>
     */
    vector<pair<int, int>>
    Select(int max_n_clips = 14, int min_clip_len = 45, int max_clip_len = 90,
           int margin_between_clips = 50) {
        min_clip_len /= rank_sample_rate_;
        max_clip_len /= rank_sample_rate_;
        margin_between_clips /= rank_sample_rate_;
        vector<int> clip_lengths;
        for (int i = 0; i < max_n_clips; ++i) {
            clip_lengths.push_back((int) random_number_between(min_clip_len, max_clip_len));
        }
        // initial search range
        vector<pair<int, int>> segments;
        segments.push_back(pair<int, int>(0, rank_.size()));
        vector<pair<int, int>> result;
        for (auto l : clip_lengths) {
            FindClipFromSegmentsGreedy(&segments, l, margin_between_clips, &result);
        }
        return result;
    }

    static double random_number_between(double from, double to) {
        double rv = (double) std::rand() / RAND_MAX;
        return from + (to - from) * rv;
    }

private:
    void FitInSegment(const pair<int, int> seg, const int l, double *max_score, int *start_point) {
        *max_score = 0;
        *start_point = 0;
        // segment is too short to fit in this clip of lenght @l
        if (seg.second - seg.first < l) {
            return;
        }
        // sliding window to find the best start_point for clip, where we can get the max score
        for (int p = seg.first; p + l <= seg.second; p++) {
            double score = dp_[p + l] - dp_[p];
            if (score > *max_score) {
                *max_score = score;
                *start_point = p;
            }
        }
    }

    void FindClipFromSegmentsGreedy(vector<pair<int, int>> *segments, const int l, const int margin,
                                    vector<pair<int, int>> *result) {
        double max_score = 0;
        int start_point = 0;
        int max_score_seg_idx = 0;
        for (int i = 0; i < segments->size(); ++i) {
            double score = 0;
            int point = 0;
            FitInSegment((*segments)[i], l, &score, &point);
            if (score > max_score) {
                max_score = score;
                start_point = point;
                max_score_seg_idx = i;
            }
        }
        // cannot find a seg to fit in clip l
        if (max_score == 0) {
            return;
        }
        // cut out the segments occupied by this clip
        auto best_seg = (*segments)[max_score_seg_idx];
        segments->erase(segments->begin() + max_score_seg_idx);
        auto left_seg = pair<int, int>(best_seg.first,
                                       std::max(best_seg.first, start_point - margin));
        auto right_seg = pair<int, int>(std::min(best_seg.second, start_point + l + margin),
                                        best_seg.second);
        if (left_seg.second - left_seg.first > 0) {
            segments->push_back(left_seg);
        }
        if (right_seg.second - right_seg.first > 0) {
            segments->push_back(right_seg);
        }
        result->push_back(pair<int, int>(start_point, start_point + l));
    }

    vector<double> rank_;
    vector<double> dp_;
    int rank_sample_rate_;
};

#endif //FINDBESTCLIPFROMRANK_CLIPSELECTOR_H