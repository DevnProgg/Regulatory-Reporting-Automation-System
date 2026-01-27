import React, { useState } from 'react';
import { BarChart, Bar, LineChart, Line, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { FileText, CheckCircle, Clock, TrendingUp, Download, Filter } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  change: string;
  icon: React.ReactNode;
  trend: 'up' | 'down' | 'neutral';
}

const MetricCard: React.FC<MetricCardProps> = ({ title, value, change, icon, trend }) => {
  const trendColor = trend === 'up' ? 'text-green-600' : trend === 'down' ? 'text-red-600' : 'text-gray-600';
  
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600 mb-1">{title}</p>
          <p className="text-2xl font-bold text-gray-900 mb-2">{value}</p>
          <p className={`text-sm font-medium ${trendColor}`}>{change}</p>
        </div>
        <div className="bg-blue-50 p-3 rounded-lg">
          {icon}
        </div>
      </div>
    </div>
  );
};

const Dashboard: React.FC = () => {
  const [selectedPeriod, setSelectedPeriod] = useState('month');
  
  // Placeholder data for submission trends
  const submissionData = [
    { month: 'Jan', submitted: 45, pending: 8, rejected: 2 },
    { month: 'Feb', submitted: 52, pending: 6, rejected: 1 },
    { month: 'Mar', submitted: 49, pending: 10, rejected: 3 },
    { month: 'Apr', submitted: 58, pending: 7, rejected: 2 },
    { month: 'May', submitted: 61, pending: 9, rejected: 1 },
    { month: 'Jun', submitted: 55, pending: 5, rejected: 2 }
  ];

  // Placeholder data for report types distribution
  const reportTypeData = [
    { name: 'AML/CTF', value: 35, color: '#2563eb' },
    { name: 'Basel III', value: 28, color: '#7c3aed' },
    { name: 'FINRA', value: 22, color: '#0891b2' },
    { name: 'MiFID II', value: 15, color: '#059669' }
  ];

  // Placeholder data for compliance timeline
  const complianceData = [
    { week: 'W1', score: 94 },
    { week: 'W2', score: 96 },
    { week: 'W3', score: 93 },
    { week: 'W4', score: 97 },
    { week: 'W5', score: 98 },
    { week: 'W6', score: 95 }
  ];

  const recentReports = [
    { id: 'REP-2024-1547', type: 'AML/CTF Report', status: 'Submitted', date: '2024-01-24', regulator: 'FinCEN' },
    { id: 'REP-2024-1546', type: 'Basel III Capital', status: 'Approved', date: '2024-01-23', regulator: 'Federal Reserve' },
    { id: 'REP-2024-1545', type: 'FINRA Rule 4524', status: 'In Review', date: '2024-01-22', regulator: 'FINRA' },
    { id: 'REP-2024-1544', type: 'MiFID II Transaction', status: 'Submitted', date: '2024-01-21', regulator: 'ESMA' },
    { id: 'REP-2024-1543', type: 'Liquidity Coverage', status: 'Pending', date: '2024-01-20', regulator: 'OCC' }
  ];

  const getStatusColor = (status: string): string => {
    switch (status) {
      case 'Approved': return 'bg-green-100 text-green-800';
      case 'Submitted': return 'bg-blue-100 text-blue-800';
      case 'In Review': return 'bg-yellow-100 text-yellow-800';
      case 'Pending': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Regulatory Reporting Analytics</h1>
              <p className="text-sm text-gray-600 mt-1">Real-time compliance monitoring and reporting dashboard</p>
            </div>
            <div className="flex items-center gap-3">
              <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 transition-colors">
                <Filter size={16} />
                Filter
              </button>
              <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors">
                <Download size={16} />
                Export Report
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8">
        {/* Metrics Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <MetricCard
            title="Total Reports Submitted"
            value="324"
            change="+12% from last month"
            icon={<FileText className="text-blue-600" size={24} />}
            trend="up"
          />
          <MetricCard
            title="Compliance Rate"
            value="97.8%"
            change="+2.1% from last month"
            icon={<CheckCircle className="text-green-600" size={24} />}
            trend="up"
          />
          <MetricCard
            title="Pending Reviews"
            value="23"
            change="-5 from last week"
            icon={<Clock className="text-yellow-600" size={24} />}
            trend="down"
          />
          <MetricCard
            title="Avg. Processing Time"
            value="2.4 days"
            change="-0.3 days improved"
            icon={<TrendingUp className="text-purple-600" size={24} />}
            trend="down"
          />
        </div>

        {/* Charts Row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* Submission Trends */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-semibold text-gray-900">Submission Trends</h2>
              <select 
                value={selectedPeriod}
                onChange={(e) => setSelectedPeriod(e.target.value)}
                className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="week">Last Week</option>
                <option value="month">Last Month</option>
                <option value="quarter">Last Quarter</option>
              </select>
            </div>
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={submissionData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="month" tick={{ fontSize: 12 }} stroke="#6b7280" />
                <YAxis tick={{ fontSize: 12 }} stroke="#6b7280" />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px' }}
                />
                <Legend wrapperStyle={{ fontSize: '12px' }} />
                <Bar dataKey="submitted" fill="#2563eb" radius={[4, 4, 0, 0]} />
                <Bar dataKey="pending" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                <Bar dataKey="rejected" fill="#ef4444" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Report Type Distribution */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-6">Report Type Distribution</h2>
            <div className="flex items-center justify-center">
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={reportTypeData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={2}
                    dataKey="value"
                  >
                    {reportTypeData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="grid grid-cols-2 gap-3 mt-4">
              {reportTypeData.map((item, index) => (
                <div key={index} className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }}></div>
                  <span className="text-xs text-gray-700 font-medium">{item.name}</span>
                  <span className="text-xs text-gray-500 ml-auto">{item.value}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Compliance Score Trend */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 mb-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-6">Compliance Score Trend</h2>
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={complianceData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="week" tick={{ fontSize: 12 }} stroke="#6b7280" />
              <YAxis domain={[90, 100]} tick={{ fontSize: 12 }} stroke="#6b7280" />
              <Tooltip 
                contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px' }}
              />
              <Line 
                type="monotone" 
                dataKey="score" 
                stroke="#2563eb" 
                strokeWidth={3}
                dot={{ fill: '#2563eb', r: 5 }}
                activeDot={{ r: 7 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Recent Reports Table */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Recent Report Submissions</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">Report ID</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">Regulator</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">Date</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-700 uppercase tracking-wider">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {recentReports.map((report, index) => (
                  <tr key={index} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 text-sm font-medium text-gray-900">{report.id}</td>
                    <td className="px-6 py-4 text-sm text-gray-700">{report.type}</td>
                    <td className="px-6 py-4 text-sm text-gray-700">{report.regulator}</td>
                    <td className="px-6 py-4 text-sm text-gray-600">{report.date}</td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getStatusColor(report.status)}`}>
                        {report.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard;