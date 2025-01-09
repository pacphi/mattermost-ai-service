// src/pages/IngestionPage.jsx
import React, { useState, useEffect } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';

const IngestionPage = ({ onNavigate }) => {
    const [channels, setChannels] = useState([]);
    const [selectedChannel, setSelectedChannel] = useState('');
    const [alert, setAlert] = useState({ show: false, message: '', type: '' });

    useEffect(() => {
        fetchChannels();
    }, []);

    const fetchChannels = async () => {
        try {
            const response = await fetch('/api/mattermost/channels');
            if (!response.ok) throw new Error('Failed to fetch channels');
            const data = await response.json();
            setChannels(data);
        } catch (error) {
            showAlert('Error fetching channels', 'error');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedChannel) {
            showAlert('Please select a channel', 'warning');
            return;
        }

        try {
            const response = await fetch(`/api/mattermost/ingest?channelId=${selectedChannel}`, {
                method: 'POST',
            });

            if (!response.ok) throw new Error('Ingestion failed');
            showAlert('Ingestion successful!', 'success');
        } catch (error) {
            showAlert('Error during ingestion', 'error');
        }
    };

    const showAlert = (message, type) => {
        setAlert({ show: true, message, type });
        setTimeout(() => setAlert({ show: false, message: '', type: '' }), 3000);
    };

    return (
        <div className="max-w-4xl mx-auto p-6">
            <h1 className="text-2xl font-bold mb-6">Mattermost Post Ingestion</h1>

            {alert.show && (
                <Alert className={`mb-4 ${alert.type === 'error' ? 'bg-red-100' : 'bg-green-100'}`}>
                    <AlertDescription>{alert.message}</AlertDescription>
                </Alert>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                    <label className="block text-sm font-medium mb-2">
                        Select Channel
                    </label>
                    <select
                        value={selectedChannel}
                        onChange={(e) => setSelectedChannel(e.target.value)}
                        className="w-full p-2 border rounded-md"
                    >
                        <option value="">Select a channel...</option>
                        {channels.map((channel) => (
                            <option key={channel.id} value={channel.id}>
                                {channel.name}
                            </option>
                        ))}
                    </select>
                </div>

                <button
                    type="submit"
                    className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
                >
                    Start Ingestion
                </button>
            </form>

            <button
                onClick={() => onNavigate('chat')}
                className="block mt-8 text-blue-500 hover:underline"
            >
                Go to Chat Interface
            </button>
        </div>
    );
};

export default IngestionPage;