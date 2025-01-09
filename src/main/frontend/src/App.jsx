// src/App.jsx
import React, { useState } from 'react';
import IngestionPage from './pages/IngestionPage';
import ChatPage from './pages/ChatPage';

const App = () => {
    const [currentPage, setCurrentPage] = useState('ingestion');

    const handleNavigate = (page) => {
        setCurrentPage(page);
    };

    return (
        <div>
            {currentPage === 'ingestion' ? (
                <IngestionPage onNavigate={handleNavigate} />
            ) : (
                <ChatPage onNavigate={handleNavigate} />
            )}
        </div>
    );
};

export default App;