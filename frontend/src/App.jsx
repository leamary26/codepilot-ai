
import { useState, useEffect, useCallback } from 'react'
import Editor from '@monaco-editor/react'
import ReactMarkdown from 'react-markdown'

import {
  Code2, Sparkles, Bug, ShieldCheck, Zap,
  Copy, Check, Download, Trash2, FileCode,
  AlertCircle, History, RefreshCw, Eye,
  EyeOff, Clock, LogOut, User, Mail, Lock
} from 'lucide-react'

import './App.css'

const languages = [
  'Java', 'Python', 'JavaScript', 'TypeScript',
  'C', 'C++', 'SQL', 'HTML', 'CSS', 'Other'
]

const editorLanguages = {
  Java: 'java',
  Python: 'python',
  JavaScript: 'javascript',
  TypeScript: 'typescript',
  C: 'c',
  'C++': 'cpp',
  SQL: 'sql',
  HTML: 'html',
  CSS: 'css',
  Other: 'plaintext'
}

function getScoreColor(score) {
  if (score === null || score === undefined) {
    return '#94a3b8'
  }

  if (score >= 80) return '#22c55e'
  if (score >= 50) return '#fbbf24'

  return '#f87171'
}

function formatDate(date) {
  return new Date(date).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

// ================================
// LOGIN AND REGISTRATION SCREEN
// ================================

function AuthScreen({ onLogin, notice }) {

  const [register, setRegister] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event) {

    event.preventDefault()

    setBusy(true)
    setError('')

    try {

      const endpoint = register
        ? '/api/auth/register'
        : '/api/auth/login'

      const body = register
        ? { name, email, password }
        : { email, password }

      const response = await fetch(endpoint, {
        method: 'POST',

        headers: {
          'Content-Type': 'application/json'
        },

        body: JSON.stringify(body)
      })

      if (!response.ok) {

        const data = await response
          .json()
          .catch(() => ({}))

        throw new Error(
          data.message ||
          (response.status === 401
            ? 'Invalid email or password.'
            : response.status === 409
              ? 'This email is already registered.'
              : 'Unable to continue. Please try again.')
        )
      }

      const data = await response.json()

      if (!data.token || !data.email) {
        throw new Error('Invalid authentication response.')
      }

      onLogin(data)

    } catch (err) {

      setError(err.message)

    } finally {

      setBusy(false)
    }
  }

  function switchMode() {
    setRegister(previous => !previous)
    setError('')
    setPassword('')
    setShowPassword(false)
  }

  return (

    <main className="auth-page">

      <div className="auth-card">

        <div className="auth-brand">

          <div className="logo">
            <Code2 size={32} />
          </div>

          <h1>CodePilot AI</h1>

          <p>AI-Powered Code Review System</p>

        </div>

        <div className="auth-heading">

          <h2>
            {register
              ? 'Create your account'
              : 'Welcome back'}
          </h2>

          <p>
            {register
              ? 'Start building better code today.'
              : 'Sign in to your personal workspace.'}
          </p>

        </div>

        {notice && (
          <div className="auth-notice" role="status">
            {notice}
          </div>
        )}

        <form
          className="auth-form"
          onSubmit={handleSubmit}
        >

          {register && (

            <label>

              Full name

              <div className="input-wrapper">

                <User size={18} />

                <input
                  type="text"
                  placeholder="Enter your full name"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  required
                  maxLength={100}
                  autoComplete="name"
                  disabled={busy}
                />

              </div>

            </label>

          )}

          <label>

            Email address

            <div className="input-wrapper">

              <Mail size={18} />

              <input
                type="email"
                placeholder="Enter your email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
                autoComplete="email"
                disabled={busy}
              />

            </div>

          </label>

          <label>

            Password

            <div className="input-wrapper">

              <Lock size={18} />

              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter your password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                minLength={register ? 8 : undefined}
                autoComplete={
                  register
                    ? 'new-password'
                    : 'current-password'
                }
                disabled={busy}
              />

              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(v => !v)}
                aria-label={
                  showPassword
                    ? 'Hide password'
                    : 'Show password'
                }
              >
                {showPassword
                  ? <EyeOff size={18} />
                  : <Eye size={18} />}
              </button>

            </div>

          </label>

          {register && (
            <p className="password-hint">
              Use at least 8 characters.
            </p>
          )}

          {error && (

            <div className="error-message" role="alert">

              <AlertCircle size={18} />

              {error}

            </div>

          )}

          <button
            type="submit"
            className="review-button"
            disabled={busy}
          >

            {busy
              ? 'Please wait...'
              : register
                ? 'Create account'
                : 'Sign in'}

          </button>

        </form>

        <p className="auth-switch">

          {register
            ? 'Already have an account?'
            : "Don't have an account?"}

          {' '}

          <button
            type="button"
            onClick={switchMode}
            disabled={busy}
          >
            {register ? 'Sign in' : 'Create account'}
          </button>

        </p>

        <div className="auth-footer">

          <ShieldCheck size={16} />

          Private review history

        </div>

      </div>

    </main>
  )
}

// ================================
// MAIN CODE REVIEW DASHBOARD
// ================================

function Dashboard({ session, onLogout }) {

  const [code, setCode] = useState('')
  const [language, setLanguage] = useState('Java')
  const [review, setReview] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [copied, setCopied] = useState(false)

  // Review history states

  const [history, setHistory] = useState([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyError, setHistoryError] = useState('')
  const [selectedReviewId, setSelectedReviewId] = useState(null)
  const [savedScore, setSavedScore] = useState(null)
  const [deletingId, setDeletingId] = useState(null)

  // Send JWT token with every protected request

  const apiFetch = useCallback(async (url, options = {}) => {

    const response = await fetch(url, {

      ...options,

      headers: {
        ...options.headers,
        Authorization: `Bearer ${session.token}`
      }

    })

    if (response.status === 401) {

      onLogout('Your session has expired. Please sign in again.')

      throw new Error('Your session has expired.')
    }

    return response

  }, [session.token, onLogout])

  // Extract quality score

  const scoreMatch = review.match(
    /(?:quality\s+)?score\s*\*{0,2}\s*:?\s*\*{0,2}\s*(\d{1,3})\s*\/\s*100/i
  )

  const parsedScore = scoreMatch
    ? Number(scoreMatch[1])
    : null

  const score = selectedReviewId !== null
    ? savedScore
    : parsedScore !== null &&
      parsedScore >= 0 &&
      parsedScore <= 100
      ? parsedScore
      : null

  const scoreColor = getScoreColor(score)

  // Fetch personal review history

  const fetchHistory = useCallback(async () => {

    setHistoryLoading(true)
    setHistoryError('')

    try {

      const response = await apiFetch('/api/history')

      if (!response.ok) {
        throw new Error('Unable to load review history.')
      }

      const data = await response.json()

      setHistory(data)

    } catch (err) {

      setHistoryError(err.message)

    } finally {

      setHistoryLoading(false)
    }

  }, [apiFetch])

  useEffect(() => {
    fetchHistory()
  }, [fetchHistory])

  // Generate AI review

  async function handleReview() {

    if (!code.trim()) {

      setError('Please enter some code to review.')

      return
    }

    setLoading(true)
    setError('')
    setReview('')
    setCopied(false)
    setSelectedReviewId(null)
    setSavedScore(null)

    try {

      const response = await apiFetch('/api/review', {

        method: 'POST',

        headers: {
          'Content-Type': 'application/json'
        },

        body: JSON.stringify({
          code,
          language
        })

      })

      if (!response.ok) {

        if (
          response.status === 502 ||
          response.status === 503
        ) {

          throw new Error(
            'The AI service is temporarily unavailable. Please try again shortly.'
          )
        }

        throw new Error(
          `Review failed (${response.status}). Please try again.`
        )
      }

      const result = await response.text()

      setReview(result)

      await fetchHistory()

    } catch (err) {

      setError(err.message)

    } finally {

      setLoading(false)
    }
  }

  // Open a saved review

  function viewReview(item) {

    setCode(item.code)
    setLanguage(item.language)
    setReview(item.review)
    setSelectedReviewId(item.id)
    setSavedScore(item.qualityScore)
    setError('')
    setCopied(false)
  }

  // Delete a saved review

  async function deleteReview(id) {

    const confirmed = window.confirm(
      `Are you sure you want to delete review #${id}?`
    )

    if (!confirmed) return

    setDeletingId(id)
    setHistoryError('')

    try {

      const response = await apiFetch(
        `/api/history/${id}`,
        {
          method: 'DELETE'
        }
      )

      if (!response.ok) {
        throw new Error('Unable to delete the review.')
      }

      setHistory(previous =>
        previous.filter(item => item.id !== id)
      )

      if (selectedReviewId === id) {
        clearCode()
      }

    } catch (err) {

      setHistoryError(err.message)

    } finally {

      setDeletingId(null)
    }
  }

  // Clear previous results

  function clearResults() {

    setReview('')
    setError('')
    setCopied(false)
    setSelectedReviewId(null)
    setSavedScore(null)
  }

  // Load sample Java program

  function loadExample() {

    setLanguage('Java')

    setCode(
      'public class Main {\n' +
      '    public static void main(String[] args) {\n' +
      '        int a = 10;\n' +
      '        int b = 0;\n' +
      '        System.out.println(a / b);\n' +
      '    }\n' +
      '}'
    )

    clearResults()
  }

  // Clear editor

  function clearCode() {

    setCode('')
    clearResults()
  }

  // Update code

  function handleCodeChange(value) {

    setCode(value || '')
    clearResults()
  }

  // Update programming language

  function handleLanguageChange(value) {

    setLanguage(value)
    clearResults()
  }

  // Copy AI review

  async function copyReview() {

    try {

      await navigator.clipboard.writeText(review)

      setCopied(true)

    } catch {

      setError('Unable to copy the review.')
    }
  }

  // Download AI review

  function downloadReview() {

    const blob = new Blob(
      [review],
      { type: 'text/markdown;charset=utf-8' }
    )

    const url = URL.createObjectURL(blob)

    const link = document.createElement('a')

    link.href = url
    link.download = 'codepilot-review.md'
    link.click()

    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }

  return (

    <div className="app">

      {/* NAVIGATION BAR */}

      <header className="navbar">

        <div className="brand">

          <div className="logo">
            <Code2 size={27} />
          </div>

          <div>

            <h1>CodePilot AI</h1>

            <p>AI-Powered Code Review System</p>

          </div>

        </div>

        <div className="nav-actions">

          <span className="ai-badge">

            <Sparkles size={15} />

            Powered by Gemini

          </span>

          <div className="user-profile">

            <div className="user-avatar">

              {(session.name || session.email)
                .charAt(0)
                .toUpperCase()}

            </div>

            <div className="user-details">

              <strong>{session.name}</strong>

              <span>{session.email}</span>

            </div>

          </div>

          <button
            className="logout-button"
            onClick={() => onLogout()}
          >

            <LogOut size={16} />

            Logout

          </button>

        </div>

      </header>

      {/* HERO SECTION */}

      <section className="hero">

        <span className="eyebrow">

          YOUR INTELLIGENT CODING ASSISTANT

        </span>

        <h2>

          Write better code.

          <br />

          <span>Build with confidence.</span>

        </h2>

        <p>

          Detect bugs, identify security risks,
          and improve your code with intelligent
          AI-powered reviews.

        </p>

      </section>

      {/* MAIN DASHBOARD */}

      <div className="dashboard-layout">

        {/* REVIEW HISTORY */}

        <aside className="history-panel">

          <div className="history-header">

            <h3 className="icon-heading">

              <History size={19} />

              Review History

            </h3>

            <button
              className="history-refresh"
              onClick={fetchHistory}
              disabled={
                historyLoading ||
                loading ||
                deletingId !== null
              }
              title="Refresh history"
              aria-label="Refresh history"
            >

              <RefreshCw
                size={16}
                className={
                  historyLoading ? 'rotating' : ''
                }
              />

            </button>

          </div>

          <div className="history-summary">

            {history.length} saved reviews

          </div>

          {historyError && (

            <div className="history-error" role="alert">

              <AlertCircle size={16} />

              {historyError}

            </div>

          )}

          {historyLoading && history.length === 0 ? (

            <div className="history-empty">

              <div className="small-spinner"></div>

              <p>Loading history...</p>

            </div>

          ) : history.length === 0 ? (

            <div className="history-empty">

              <History size={32} />

              <h4>No reviews yet</h4>

              <p>

                Your saved AI reviews will
                appear here.

              </p>

            </div>

          ) : (

            <div className="history-list">

              {history.map(item => (

                <div
                  key={item.id}
                  className={`history-item ${
                    selectedReviewId === item.id
                      ? 'history-item-active'
                      : ''
                  }`}
                >

                  <div className="history-item-top">

                    <div className="history-file-icon">

                      <FileCode size={18} />

                    </div>

                    <div className="history-item-info">

                      <h4>{item.language} Review</h4>

                      <span>#{item.id}</span>

                    </div>

                    <span
                      className="history-score"
                      style={{
                        color: getScoreColor(item.qualityScore)
                      }}
                    >

                      {item.qualityScore !== null
                        ? `${item.qualityScore}/100`
                        : 'N/A'}

                    </span>

                  </div>

                  <div className="history-date">

                    <Clock size={12} />

                    {formatDate(item.createdAt)}

                  </div>

                  {item.qualityScore !== null && (

                    <div className="history-score-track">

                      <div
                        className="history-score-fill"
                        style={{
                          width: `${item.qualityScore}%`,
                          background: getScoreColor(
                            item.qualityScore
                          )
                        }}
                      />

                    </div>

                  )}

                  <div className="history-preview">

                    {item.code}

                  </div>

                  <div className="history-actions">

                    <button
                      className="history-view"
                      onClick={() => viewReview(item)}
                      disabled={
                        loading ||
                        deletingId !== null
                      }
                    >

                      <Eye size={14} />

                      View Review

                    </button>

                    <button
                      className="history-delete"
                      onClick={() => deleteReview(item.id)}
                      disabled={
                        loading ||
                        deletingId !== null ||
                        historyLoading
                      }
                      title="Delete review"
                      aria-label={`Delete review ${item.id}`}
                    >

                      {deletingId === item.id
                        ? '...'
                        : <Trash2 size={15} />}

                    </button>

                  </div>

                </div>

              ))}

            </div>

          )}

          <div className="history-footer">

            <ShieldCheck size={14} />

            Private to your account

          </div>

        </aside>

        {/* SOURCE CODE AND RESULTS */}

        <main className="workspace">

          {/* SOURCE CODE PANEL */}

          <section className="panel">

            <div className="panel-header">

              <h3 className="icon-heading">

                <FileCode size={19} />

                Source Code

              </h3>

              <select
                value={language}
                onChange={e =>
                  handleLanguageChange(e.target.value)
                }
                disabled={loading}
                aria-label="Programming language"
              >

                {languages.map(lang => (

                  <option key={lang} value={lang}>

                    {lang}

                  </option>

                ))}

              </select>

            </div>

            {selectedReviewId !== null && (

              <div className="saved-review-banner">

                <History size={15} />

                Viewing saved review #{selectedReviewId}

              </div>

            )}

            <div className="editor-toolbar">

              <button
                onClick={loadExample}
                disabled={loading}
              >

                <FileCode size={14} />

                Load Example

              </button>

              <button
                onClick={clearCode}
                disabled={loading}
              >

                <Trash2 size={14} />

                Clear

              </button>

            </div>

            <div className="monaco-wrapper">

              <Editor
                height="380px"
                language={
                  editorLanguages[language] || 'plaintext'
                }
                theme="vs-dark"
                value={code}
                onChange={handleCodeChange}
                loading="Loading code editor..."
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  lineNumbers: 'on',
                  automaticLayout: true,
                  scrollBeyondLastLine: false,
                  wordWrap: 'on',
                  tabSize: 4,
                  padding: { top: 16 },
                  readOnly: loading,
                  ariaLabel: 'Source code editor'
                }}
              />

            </div>

            <div className="editor-footer">

              <span>

                {code ? code.split('\n').length : 0} lines

              </span>

              <span>

                {code.length} characters

              </span>

            </div>

            <button
              className="review-button"
              onClick={handleReview}
              disabled={loading}
            >

              <Sparkles size={19} />

              {loading
                ? 'Analyzing your code...'
                : 'Review Code'}

            </button>

            {error && (

              <div className="error-message" role="alert">

                <AlertCircle size={18} />

                {error}

              </div>

            )}

          </section>

          {/* AI RESULTS PANEL */}

          <section className="panel results-panel">

            <div className="panel-header">

              <h3 className="icon-heading">

                <Sparkles size={19} />

                AI Review Results

              </h3>

              {review && (

                <div className="result-actions">

                  <button
                    className="copy-button"
                    onClick={copyReview}
                    title="Copy review"
                    aria-label="Copy review"
                  >

                    {copied
                      ? <Check size={16} />
                      : <Copy size={16} />}

                  </button>

                  <button
                    className="copy-button"
                    onClick={downloadReview}
                    title="Download review"
                    aria-label="Download review"
                  >

                    <Download size={16} />

                  </button>

                </div>

              )}

            </div>

            {loading ? (

              <div className="empty-state">

                <div className="spinner"></div>

                <h3>Analyzing your code</h3>

                <p>

                  Gemini is checking your code for
                  bugs, security risks and improvements.

                </p>

              </div>

            ) : review ? (

              <>

                {score !== null && (

                  <div className="score-card">

                    <div className="score-top">

                      <div>

                        <p>Code Quality Score</p>

                        <div
                          className="score-number"
                          style={{ color: scoreColor }}
                        >

                          {score}

                          <span>/100</span>

                        </div>

                      </div>

                      <div
                        className="score-ring"
                        style={{
                          background: `conic-gradient(
                            ${scoreColor} ${score}%,
                            #263247 ${score}%
                          )`
                        }}
                      >

                        <div className="score-ring-inner">

                          {score}%

                        </div>

                      </div>

                    </div>

                    <div className="score-track">

                      <div
                        className="score-fill"
                        style={{
                          width: `${score}%`,
                          background: scoreColor
                        }}
                      />

                    </div>

                    <p className="score-label">

                      {score >= 80
                        ? 'Good code quality'
                        : score >= 50
                          ? 'Needs improvement'
                          : 'Significant issues detected'}

                    </p>

                  </div>

                )}

                <div className="review-content markdown">

                  <ReactMarkdown
                    components={{
                      img: () => null
                    }}
                  >
                    {review}
                  </ReactMarkdown>

                </div>

              </>

            ) : (

              <div className="empty-state">

                <div className="empty-icon">

                  <Sparkles size={35} />

                </div>

                <h3>Ready to review</h3>

                <p>

                  Paste your code and click Review Code
                  to receive intelligent AI analysis.

                </p>

              </div>

            )}

          </section>

        </main>

      </div>

      {/* FEATURES SECTION */}

      <section className="features">

        <div className="feature">

          <Bug size={27} color="#f87171" />

          <h4>Bug Detection</h4>

          <p>

            Find errors before they cause problems.

          </p>

        </div>

        <div className="feature">

          <ShieldCheck size={27} color="#34d399" />

          <h4>Security Analysis</h4>

          <p>

            Identify potential security vulnerabilities.

          </p>

        </div>

        <div className="feature">

          <Zap size={27} color="#fbbf24" />

          <h4>Code Optimization</h4>

          <p>

            Get smarter suggestions for better code.

          </p>

        </div>

      </section>

      <footer>

        CodePilot AI · Built with React,
        Spring Boot, MySQL & Gemini AI

      </footer>

    </div>

  )
}

// ================================
// MAIN APPLICATION
// ================================

function App() {

  const [session, setSession] = useState(null)
  const [notice, setNotice] = useState('')

  const logout = useCallback((message = '') => {

    setSession(null)
    setNotice(message)

  }, [])

  function handleLogin(data) {

    setNotice('')
    setSession(data)
  }

  return session ? (

    <Dashboard
      key={session.token}
      session={session}
      onLogout={logout}
    />

  ) : (

    <AuthScreen
      onLogin={handleLogin}
      notice={notice}
    />

  )
}

export default App
