// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return new ArrayList<TimeRange>();
    }
    Set<String> required_attendees = get_required_attendees(request);
    ArrayList<TimeRange> unavailable_times = get_unavailable_times(
      required_attendees, events);
    return find_available_times(request.getDuration(), unavailable_times);

    //ActiveQuery query = new ActiveQuery(request);
    //ArrayList<TimeRange> unavailable_times = query.get_unavailable_times(events);
    //return query.find_available_times(unavailable_times);
  }

  Set<String> get_required_attendees(MeetingRequest request) {
    return new HashSet<>(request.getAttendees());
  }

  ArrayList<TimeRange> get_unavailable_times(Set<String> required_attendees, Collection<Event> events) {
    ArrayList<TimeRange> unavailable_times = new ArrayList<>();
    for (Event event : events) {
      if (event_is_applicable(required_attendees, event)) {
        unavailable_times.add(event.getWhen());
      }
    }

    return consolidate_time_ranges(unavailable_times);
  }

  boolean event_is_applicable(Set<String> required_attendees, Event event) {
    for (String attendee : event.getAttendees()) {
      if (required_attendees.contains(attendee)) {
        return true;
      }
    }
    return false;
  }

  ArrayList<TimeRange> consolidate_time_ranges(ArrayList<TimeRange> times) {
    if (times.size() <= 1) {
      return times;
    }

    ArrayList<TimeRange> consolidated_times = new ArrayList<>();
    Collections.sort(times, TimeRange.ORDER_BY_START);

    TimeRange consolidated_time = times.get(0);
    for (int i = 1; i < times.size(); i++) {
      TimeRange next_time = times.get(i);
      
      if (consolidated_time.overlaps(next_time)) {
        if (next_time.end() > consolidated_time.end()) {
          int new_duration = next_time.end() - consolidated_time.start();
          consolidated_time = TimeRange.fromStartDuration(
            consolidated_time.start(), new_duration);
        }
      } else {
        consolidated_times.add(consolidated_time);
        consolidated_time = next_time;
      }
    }

    consolidated_times.add(consolidated_time);

    return consolidated_times;
  }

  ArrayList<TimeRange> find_available_times(long required_duration, ArrayList<TimeRange> unavailable_times) {
    ArrayList<TimeRange> available_times = new ArrayList<>();

    int start = TimeRange.START_OF_DAY;

    if (unavailable_times.size() <= 0) {
      available_times.add(TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY, true));
      return available_times;
    }

    for (TimeRange unavailable_time : unavailable_times) {
      TimeRange range = TimeRange.fromStartEnd(start, unavailable_time.start(), false);
      if (range.duration() >= required_duration) {
        available_times.add(range);
      }
      start = unavailable_time.end();
    }

    TimeRange last_unavailable_time = unavailable_times.get(unavailable_times.size() - 1);
    if (last_unavailable_time.end() != TimeRange.END_OF_DAY) {
      TimeRange range = TimeRange.fromStartEnd(start, TimeRange.END_OF_DAY, true);
      if (range.duration() >= required_duration) {
        available_times.add(range);
      }
    }

    return available_times;
  }
}
