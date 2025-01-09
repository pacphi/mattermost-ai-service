// src/pages/ChatPage.jsx
import React, { useState, useRef } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';

const ChatPage = ({ onNavigate }) => {
    const [question, setQuestion] = useState('');
    const [answer, setAnswer] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [alert, setAlert] = useState({ show: false, message: '' });
    const answerContainerRef = useRef(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!question.trim()) {
            showAlert('Please enter a question');
            return;
        }

        setIsLoading(true);
        setAnswer('');

        try {
            const response = await fetch('/api/mattermost/stream/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    question: question,
                    filter: []
                }),
            });

            if (!response.ok) throw new Error('Chat request failed');

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value);
                setAnswer(prev => prev + chunk);

                if (answerContainerRef.current) {
                    answerContainerRef.current.scrollTop = answerContainerRef.current.scrollHeight;
                }
            }
        } catch (error) {
            showAlert('Error processing chat request');
        } finally {
            setIsLoading(false);
        }
    };

    const showAlert = (message) => {
        setAlert({ show: true, message });
        setTimeout(() => setAlert({ show: false, message: '' }), 3000);
    };

    return (
        <div className="max-w-4xl mx-auto p-6">
            <h1 className="text-2xl font-bold mb-6">Chat Interface</h1>

            {alert.show && (
                <Alert className="mb-4 bg-red-100">
                    <AlertDescription>{alert.message}</AlertDescription>
                </Alert>
            )}

            <div
                ref={answerContainerRef}
                className="bg-gray-50 p-4 rounded-lg mb-4 h-96 overflow-y-auto"
            >
                {answer || 'Response will appear here...'}
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
                <div>
          <textarea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Enter your question..."
              className="w-full p-2 border rounded-md h-32"
              disabled={isLoading}
          />
                </div>

                <button
                    type="submit"
                    className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:bg-gray-400"
                    disabled={isLoading}
                >
                    {isLoading ? 'Processing...' : 'Submit'}
                </button>
            </form>

            <button
                onClick={() => onNavigate('ingestion')}
                className="block mt-8 text-blue-500 hover:underline"
            >
                Back to Ingestion
            </button>
        </div>
    );
};

export default ChatPage;