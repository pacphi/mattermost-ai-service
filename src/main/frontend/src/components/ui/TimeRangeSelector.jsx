import React, { useState } from 'react';

const TimeRangeSelector = ({ onValueChange, disabled }) => {
    const [selectedRange, setSelectedRange] = useState('');

    const calculateTimestamp = (range) => {
        const now = new Date();
        const timestamp = new Date(now);

        switch (range) {
            // Minutes
            case 'past_5_minutes':
                timestamp.setMinutes(now.getMinutes() - 5);
                break;
            case 'past_10_minutes':
                timestamp.setMinutes(now.getMinutes() - 10);
                break;
            case 'past_15_minutes':
                timestamp.setMinutes(now.getMinutes() - 15);
                break;
            case 'past_30_minutes':
                timestamp.setMinutes(now.getMinutes() - 30);
                break;
            // Hours
            case 'past_hour':
                timestamp.setHours(now.getHours() - 1);
                break;
            case 'past_two_hours':
                timestamp.setHours(now.getHours() - 2);
                break;
            case 'past_six_hours':
                timestamp.setHours(now.getHours() - 6);
                break;
            case 'past_twelve_hours':
                timestamp.setHours(now.getHours() - 12);
                break;
            // Days
            case 'past_day':
                timestamp.setDate(now.getDate() - 1);
                break;
            case 'past_two_days':
                timestamp.setDate(now.getDate() - 2);
                break;
            case 'past_week':
                timestamp.setDate(now.getDate() - 7);
                break;
            case 'past_two_weeks':
                timestamp.setDate(now.getDate() - 14);
                break;
            // Months and Years
            case 'past_month':
                timestamp.setMonth(now.getMonth() - 1);
                break;
            case 'past_quarter':
                timestamp.setMonth(now.getMonth() - 3);
                break;
            case 'past_six_months':
                timestamp.setMonth(now.getMonth() - 6);
                break;
            case 'past_year':
                timestamp.setFullYear(now.getFullYear() - 1);
                break;
            case 'past_two_years':
                timestamp.setFullYear(now.getFullYear() - 2);
                break;
            case 'past_four_years':
                timestamp.setFullYear(now.getFullYear() - 4);
                break;
            case 'past_five_years':
                timestamp.setFullYear(now.getFullYear() - 5);
                break;
            case 'past_ten_years':
                timestamp.setFullYear(now.getFullYear() - 10);
                break;
            default:
                return null;
        }

        return timestamp.getTime();
    };

    const timeRanges = [
        // Minutes
        { value: 'past_5_minutes', label: 'Within the past 5 minutes' },
        { value: 'past_10_minutes', label: 'Within the past 10 minutes' },
        { value: 'past_15_minutes', label: 'Within the past 15 minutes' },
        { value: 'past_30_minutes', label: 'Within the past 30 minutes' },
        // Hours
        { value: 'past_hour', label: 'Within the past hour' },
        { value: 'past_two_hours', label: 'Within the past two hours' },
        { value: 'past_six_hours', label: 'Within the past six hours' },
        { value: 'past_twelve_hours', label: 'Within the past twelve hours' },
        // Days
        { value: 'past_day', label: 'Within the past day' },
        { value: 'past_two_days', label: 'Within the past two days' },
        { value: 'past_week', label: 'Within the past week' },
        { value: 'past_two_weeks', label: 'Within the past two weeks' },
        // Months and Years
        { value: 'past_month', label: 'Within the past month' },
        { value: 'past_quarter', label: 'Within the past quarter' },
        { value: 'past_six_months', label: 'Within the past six months' },
        { value: 'past_year', label: 'Within the past year' },
        { value: 'past_two_years', label: 'Within the past two years' },
        { value: 'past_four_years', label: 'Within the past four years' },
        { value: 'past_five_years', label: 'Within the past five years' },
        { value: 'past_ten_years', label: 'Within the past ten years' }
    ];

    const handleChange = (e) => {
        const value = e.target.value;
        setSelectedRange(value);
        const timestamp = calculateTimestamp(value);
        onValueChange(timestamp);
    };

    return (
        <select
            value={selectedRange}
            onChange={handleChange}
            className="w-full p-2 border rounded-md"
            disabled={disabled}
        >
            <option value="">Select time range...</option>
            {timeRanges.map(({ value, label }) => (
                <option key={value} value={value}>
                    {label}
                </option>
            ))}
        </select>
    );
};

export default TimeRangeSelector;