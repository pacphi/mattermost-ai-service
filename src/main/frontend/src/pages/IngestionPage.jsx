import React, { useState, useEffect } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Loader2 } from 'lucide-react';

const IngestionPage = ({ onNavigate }) => {
    const [channels, setChannels] = useState([]);
    const [selectedChannel, setSelectedChannel] = useState('');
    const [alert, setAlert] = useState({ show: false, message: '', type: '' });
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        fetchChannels();
    }, []);

    const fetchChannels = async () => {
        setIsLoading(true);
        try {
            const response = await fetch('/api/mattermost/channels');
            if (!response.ok) throw new Error('Failed to fetch channels');
            const data = await response.json();
            setChannels(data);
        } catch (error) {
            showAlert('Error fetching channels', 'error');
        } finally {
            setIsLoading(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedChannel) {
            showAlert('Please select a channel', 'warning');
            return;
        }

        setIsLoading(true);
        try {
            const response = await fetch(`/api/mattermost/ingest?channelId=${selectedChannel}`, {
                method: 'POST',
            });

            if (!response.ok) throw new Error('Ingestion failed');
            showAlert('Ingestion successful!', 'success');
        } catch (error) {
            showAlert('Error during ingestion', 'error');
        } finally {
            setIsLoading(false);
        }
    };

    const showAlert = (message, type) => {
        setAlert({ show: true, message, type });
        setTimeout(() => setAlert({ show: false, message: '', type: '' }), 3000);
    };

    return (
        <div className="max-w-4xl mx-auto p-6">
            <h1 className="text-2xl font-bold mb-6">Mattermost AI Service: Channel Posts Ingestion Interface</h1>

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
                        disabled={isLoading}
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
                    className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center min-w-32"
                    disabled={isLoading}
                >
                    {isLoading ? (
                        <>
                            <svg
                                className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                                xmlns="http://www.w3.org/2000/svg"
                                fill="none"
                                viewBox="0 0 24 24"
                            >
                                <circle
                                    className="opacity-25"
                                    cx="12"
                                    cy="12"
                                    r="10"
                                    stroke="currentColor"
                                    strokeWidth="4"
                                ></circle>
                                <path
                                    className="opacity-75"
                                    fill="currentColor"
                                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                ></path>
                            </svg>
                            Processing...
                        </>
                    ) : (
                        'Start Ingestion'
                    )}
                </button>
            </form>

            <button
                onClick={() => onNavigate('chat')}
                className="block mt-8 text-blue-500 hover:underline"
                disabled={isLoading}
            >
                Go to Chat Interface
            </button>
        </div>
    );
};

export default IngestionPage;