import React from 'react';

const LoadingDots = () => (
    <div className="host">
        <div className="loading loading-0"></div>
        <div className="loading loading-1"></div>
        <div className="loading loading-2"></div>

        <style jsx>{`
      .host{
        padding: 8px 2px 8px 2px;
      }

      .loading{
        width:8px;
        height:8px;
        background:#FFF;
        border-radius:100%;
        float:left;
        margin-right:5px;
      }

      .loading-0{
        animation:bounce 2s infinite;
        animation-delay:.2s;
        background: black;
      }

      .loading-1{
        animation:bounce 2s infinite;
        animation-delay:.6s;
        background: black;
      }

      .loading-2{
        animation:bounce 2s infinite ease;
        animation-delay: 1s;
        background: black;
      }

      @keyframes bounce {
        0%, 100% {
          opacity:1;
        }
        60% {
          opacity:0;
        }
      }
    `}</style>
    </div>
);

export default LoadingDots;
