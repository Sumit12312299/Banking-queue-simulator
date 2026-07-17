import React, { useState, useEffect, useRef } from 'react';
import { 
  Users, UserCheck, UserPlus, Sliders, RefreshCw, 
  ShieldAlert, Trash2, CheckCircle, Activity, 
  BarChart3, Play, Pause, Terminal, Clock, Star, ArrowRight
} from 'lucide-react';
import './App.css';

// Random Name Lists for Customer Generation
const FIRST_NAMES = ["Sumit", "Amit", "Rahul", "Priya", "Anjali", "Vikram", "Sneha", "Karan", "Pooja", "Rohit", "Deepak", "Neha", "Aarav", "Kabir", "Meera", "Sanjay", "Kiran", "Aditya", "Riya", "Rajesh"];
const LAST_NAMES = ["Kumar", "Singh", "Sharma", "Verma", "Gupta", "Patel", "Mehta", "Joshi", "Das", "Sen", "Rao", "Reddy", "Choudhury", "Bose", "Nair", "Mishra"];

const VIP_TIERS = {
  VVIP: { level: 1, label: "VVIP (High Priority)" },
  VIP: { level: 2, label: "VIP (Medium Priority)" },
  PREFERRED: { level: 3, label: "Preferred (Low Priority)" }
};

function App() {
  // --- Queues and Counters ---
  const [vipQueue, setVipQueue] = useState([]);
  const [regularQueue, setRegularQueue] = useState([]);
  const [servedCustomers, setServedCustomers] = useState([]);
  
  // --- Tellers State ---
  const [tellers, setTellers] = useState([
    { id: 1, name: "Counter A (Vip & Regular)", busy: false, currentCustomer: null, serviceTimeRemaining: 0, serviceDuration: 0, servedCount: 0 },
    { id: 2, name: "Counter B (Vip & Regular)", busy: false, currentCustomer: null, serviceTimeRemaining: 0, serviceDuration: 0, servedCount: 0 },
    { id: 3, name: "Counter C (Standard Queue)", busy: false, currentCustomer: null, serviceTimeRemaining: 0, serviceDuration: 0, servedCount: 0 }
  ]);

  // --- Policy Controls ---
  const [starvePreventionEnabled, setStarvePreventionEnabled] = useState(true);
  const [maxConsecutiveVips, setMaxConsecutiveVips] = useState(3);
  const [consecutiveVipsServed, setConsecutiveVipsServed] = useState(0);

  // --- Manual Form Inputs ---
  const [formName, setFormName] = useState("");
  const [formCategory, setFormCategory] = useState("REGULAR");
  const [formVipTier, setFormVipTier] = useState("VIP");

  // --- Simulation Settings ---
  const [autoSimEnabled, setAutoSimEnabled] = useState(false);
  const [arrivalRate, setArrivalRate] = useState(2); // every X seconds
  const [minServiceTime, setMinServiceTime] = useState(3); // min serving duration
  const [maxServiceTime, setMaxServiceTime] = useState(7); // max serving duration
  const [vipProbability, setVipProbability] = useState(30); // VIP chance in %

  // --- Live Event Logs ---
  const [logs, setLogs] = useState([]);
  
  // --- Refs for State sync (Strict Mode safe & stale closure proof) ---
  const vipQueueRef = useRef([]);
  const regularQueueRef = useRef([]);
  const consecutiveVipsServedRef = useRef(0);
  const vipTokenCounterRef = useRef(0);
  const regularTokenCounterRef = useRef(0);
  const tellersRef = useRef([]);

  // Sync refs with state
  useEffect(() => {
    vipQueueRef.current = vipQueue;
  }, [vipQueue]);

  useEffect(() => {
    regularQueueRef.current = regularQueue;
  }, [regularQueue]);

  useEffect(() => {
    consecutiveVipsServedRef.current = consecutiveVipsServed;
  }, [consecutiveVipsServed]);

  useEffect(() => {
    tellersRef.current = tellers;
  }, [tellers]);

  // --- Refs for auto scrolling ---
  const consoleLogsRef = useRef(null);
  const timerRef = useRef(null);
  const arrivalTimerRef = useRef(0);

  // Add a log entry
  const addLog = (message, type = "system") => {
    const timestamp = new Date().toLocaleTimeString();
    setLogs(prevLogs => [...prevLogs, { timestamp, message, type }]);
  };

  // Scroll logs to bottom (only inside the scroll container, without jumping the page)
  useEffect(() => {
    if (consoleLogsRef.current) {
      consoleLogsRef.current.scrollTop = consoleLogsRef.current.scrollHeight;
    }
  }, [logs]);

  // --- Enqueue Functions ---
  const addCustomer = (name, category, tier = "VIP") => {
    const customerName = name.trim() || "Guest Customer";
    const timestamp = Date.now();

    if (category === "VIP") {
      vipTokenCounterRef.current += 1;
      const nextTokenNum = vipTokenCounterRef.current;
      const token = `VIP-${String(nextTokenNum).padStart(3, '0')}`;
      const vipTierInfo = VIP_TIERS[tier];
      
      const newCustomer = {
        id: `VIP-${timestamp}-${nextTokenNum}`,
        name: customerName,
        tokenNumber: token,
        type: "VIP",
        vipTier: tier,
        priorityLevel: vipTierInfo.level,
        arrivalTime: timestamp,
        serviceStartTime: null,
        serviceDuration: 0
      };

      setVipQueue(prevQueue => {
        const sorted = [...prevQueue, newCustomer].sort((a, b) => {
          if (a.priorityLevel !== b.priorityLevel) {
            return a.priorityLevel - b.priorityLevel; // lower level number = higher priority
          }
          return a.arrivalTime - b.arrivalTime; // FIFO for same priority tier
        });
        return sorted;
      });

      addLog(`Token ${token} issued to VIP (${tier}) - ${customerName}`, "arrival");
    } else {
      regularTokenCounterRef.current += 1;
      const nextTokenNum = regularTokenCounterRef.current;
      const token = `REG-${String(nextTokenNum).padStart(3, '0')}`;
      
      const newCustomer = {
        id: `REG-${timestamp}-${nextTokenNum}`,
        name: customerName,
        tokenNumber: token,
        type: "REGULAR",
        vipTier: null,
        priorityLevel: Infinity,
        arrivalTime: timestamp,
        serviceStartTime: null,
        serviceDuration: 0
      };

      setRegularQueue(prevQueue => [...prevQueue, newCustomer]);
      addLog(`Token ${token} issued to Regular - ${customerName}`, "arrival");
    }
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    addCustomer(formName, formCategory, formVipTier);
    setFormName("");
  };

  // --- Queue Policy Handler (Dequeue) ---
  const fetchNextCustomer = (currentVipQueue, currentRegularQueue, currentConsecutiveVips) => {
    if (currentVipQueue.length === 0 && currentRegularQueue.length === 0) {
      return { customer: null, nextVips: [], nextRegulars: [], nextConsecutiveCount: currentConsecutiveVips };
    }

    // Copy queues to mutate
    let nextVips = [...currentVipQueue];
    let nextRegulars = [...currentRegularQueue];
    let customer = null;
    let nextConsecutiveCount = currentConsecutiveVips;

    // Decoupled selection logic matching Java implementation
    if (nextVips.length > 0) {
      let serveVip = true;

      if (starvePreventionEnabled && nextRegulars.length > 0 && currentConsecutiveVips >= maxConsecutiveVips) {
        serveVip = false; // Trigger starve prevention
        addLog(`[Scheduling Policy] Starve prevention triggered! Serving Regular customer after ${currentConsecutiveVips} VIPs.`, "vip-policy");
      }

      if (serveVip) {
        customer = nextVips.shift();
        nextConsecutiveCount = currentConsecutiveVips + 1;
      } else {
        customer = nextRegulars.shift();
        nextConsecutiveCount = 0; // Reset
      }
    } else {
      customer = nextRegulars.shift();
      nextConsecutiveCount = 0; // Reset
    }

    if (customer) {
      customer.serviceStartTime = Date.now();
    }

    return { customer, nextVips, nextRegulars, nextConsecutiveCount };
  };

  // --- Serve Step (Manual or Triggered) ---
  const serveNextForTeller = (tellerId) => {
    const currentVips = vipQueueRef.current;
    const currentRegulars = regularQueueRef.current;
    const currentConsecutive = consecutiveVipsServedRef.current;

    const result = fetchNextCustomer(currentVips, currentRegulars, currentConsecutive);
    const customerToServe = result.customer;

    if (!customerToServe) {
      addLog(`Teller could not find any waiting customers.`, "system");
      return;
    }

    // Update states cleanly and directly
    setVipQueue(result.nextVips);
    setRegularQueue(result.nextRegulars);
    setConsecutiveVipsServed(result.nextConsecutiveCount);

    const nextTellers = tellersRef.current.map(t => {
      if (t.id === tellerId) {
        const randDuration = Math.floor(Math.random() * (maxServiceTime - minServiceTime + 1)) + minServiceTime;
        addLog(`${t.name} started serving ${customerToServe.name} (${customerToServe.tokenNumber}). Wait: ${((Date.now() - customerToServe.arrivalTime) / 1000).toFixed(1)}s`, "serving");
        return {
          ...t,
          busy: true,
          currentCustomer: customerToServe,
          serviceTimeRemaining: randDuration,
          serviceDuration: randDuration
        };
      }
      return t;
    });

    setTellers(nextTellers);
  };

  // --- Manual Teller Complete ---
  const completeTellerService = (tellerId) => {
    const teller = tellers.find(t => t.id === tellerId);
    if (!teller || !teller.busy) return;

    const customer = teller.currentCustomer;
    const serviceTimeMs = (teller.serviceDuration - teller.serviceTimeRemaining) * 1000;
    customer.serviceDuration = serviceTimeMs;

    setServedCustomers(prevServed => [...prevServed, customer]);
    addLog(`${teller.name} completed serving ${customer.name} (${customer.tokenNumber}). Service: ${(serviceTimeMs / 1000).toFixed(1)}s. Wait: ${((customer.serviceStartTime - customer.arrivalTime) / 1000).toFixed(1)}s`, "served");

    setTellers(prevTellers => prevTellers.map(t => {
      if (t.id === tellerId) {
        return {
          ...t,
          busy: false,
          currentCustomer: null,
          serviceTimeRemaining: 0,
          serviceDuration: 0,
          servedCount: t.servedCount + 1
        };
      }
      return t;
    }));
  };

  // --- Populate Dummy Data ---
  const populateTestQueues = () => {
    addCustomer("Sumit Kumar", "REGULAR");
    addCustomer("Amit Sharma", "VIP", "VIP");
    addCustomer("Rahul Patel", "VIP", "VVIP");
    addCustomer("Priya Singh", "REGULAR");
    addCustomer("Anjali Verma", "VIP", "PREFERRED");
    addCustomer("Vikram Gupta", "VIP", "VVIP");
    addCustomer("Karan Mehta", "REGULAR");
    addLog("Added 7 test customers to queues (3 Regular, 4 VIPs).", "system");
  };

  // --- Reset All State ---
  const resetSimulator = () => {
    vipTokenCounterRef.current = 0;
    regularTokenCounterRef.current = 0;
    vipQueueRef.current = [];
    regularQueueRef.current = [];
    consecutiveVipsServedRef.current = 0;

    setVipQueue([]);
    setRegularQueue([]);
    setServedCustomers([]);
    setConsecutiveVipsServed(0);
    setTellers(prev => prev.map(t => ({
      ...t,
      busy: false,
      currentCustomer: null,
      serviceTimeRemaining: 0,
      serviceDuration: 0,
      servedCount: 0
    })));
    setLogs([]);
    addLog("Simulator queues and counters reset successfully.", "system");
  };

  // --- Auto-Simulation Core Loop ---
  useEffect(() => {
    if (autoSimEnabled) {
      addLog("Automated Bank Simulation is now RUNNING.", "system");
      
      timerRef.current = setInterval(() => {
        let currentVips = [...vipQueueRef.current];
        let currentRegulars = [...regularQueueRef.current];
        let currentConsec = consecutiveVipsServedRef.current;
        let currentTellers = [...tellersRef.current];
        let queuesChanged = false;
        let servedList = [];
        let logsList = [];

        // 1. Tick service timer on all busy tellers
        let updatedTellers = currentTellers.map(t => {
          if (t.busy) {
            const remaining = t.serviceTimeRemaining - 1;
            if (remaining <= 0) {
              // Completed service
              const customer = { ...t.currentCustomer };
              customer.serviceDuration = t.serviceDuration * 1000;
              servedList.push(customer);
              
              logsList.push({
                message: `${t.name} completed serving ${customer.name} (${customer.tokenNumber}). Service: ${t.serviceDuration}s. Wait: ${((customer.serviceStartTime - customer.arrivalTime) / 1000).toFixed(1)}s`,
                type: "served"
              });

              return {
                ...t,
                busy: false,
                currentCustomer: null,
                serviceTimeRemaining: 0,
                serviceDuration: 0,
                servedCount: t.servedCount + 1
              };
            } else {
              return { ...t, serviceTimeRemaining: remaining };
            }
          }
          return t;
        });

        // 2. Assign waiting customers to idle tellers
        updatedTellers = updatedTellers.map(t => {
          if (!t.busy) {
            const result = fetchNextCustomer(currentVips, currentRegulars, currentConsec);
            const customerToServe = result.customer;

            if (customerToServe) {
              currentVips = result.nextVips;
              currentRegulars = result.nextRegulars;
              currentConsec = result.nextConsecutiveCount;
              queuesChanged = true;

              const randDuration = Math.floor(Math.random() * (maxServiceTime - minServiceTime + 1)) + minServiceTime;
              
              logsList.push({
                message: `${t.name} started serving ${customerToServe.name} (${customerToServe.tokenNumber}). Wait: ${((Date.now() - customerToServe.arrivalTime) / 1000).toFixed(1)}s`,
                type: "serving"
              });

              return {
                ...t,
                busy: true,
                currentCustomer: customerToServe,
                serviceTimeRemaining: randDuration,
                serviceDuration: randDuration
              };
            }
          }
          return t;
        });

        setTellers(updatedTellers);

        if (servedList.length > 0) {
          setServedCustomers(prevServed => [...prevServed, ...servedList]);
        }

        if (queuesChanged) {
          setVipQueue(currentVips);
          setRegularQueue(currentRegulars);
          setConsecutiveVipsServed(currentConsec);
        }

        logsList.forEach(logInfo => {
          addLog(logInfo.message, logInfo.type);
        });

        // 3. Tick customer arrivals
        arrivalTimerRef.current += 1;
        if (arrivalTimerRef.current >= arrivalRate) {
          arrivalTimerRef.current = 0;
          
          // Generate a customer
          const isVip = Math.random() < (vipProbability / 100);
          const fName = FIRST_NAMES[Math.floor(Math.random() * FIRST_NAMES.length)];
          const lName = LAST_NAMES[Math.floor(Math.random() * LAST_NAMES.length)];
          const name = `${fName} ${lName}`;

          if (isVip) {
            const tiers = Object.keys(VIP_TIERS);
            const tier = tiers[Math.floor(Math.random() * tiers.length)];
            addCustomer(name, "VIP", tier);
          } else {
            addCustomer(name, "REGULAR");
          }
        }

      }, 1000);
    } else {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        addLog("Automated Bank Simulation is PAUSED.", "system");
      }
    }

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [autoSimEnabled, arrivalRate, minServiceTime, maxServiceTime, vipProbability, starvePreventionEnabled, maxConsecutiveVips]);

  // --- Statistics Calculation ---
  const calculateStats = () => {
    const total = servedCustomers.length;
    const vips = servedCustomers.filter(c => c.type === "VIP");
    const regulars = servedCustomers.filter(c => c.type === "REGULAR");

    const totalWait = servedCustomers.reduce((acc, c) => acc + (c.serviceStartTime - c.arrivalTime), 0);
    const vipWait = vips.reduce((acc, c) => acc + (c.serviceStartTime - c.arrivalTime), 0);
    const regWait = regulars.reduce((acc, c) => acc + (c.serviceStartTime - c.arrivalTime), 0);

    const maxWait = servedCustomers.reduce((max, c) => {
      const wait = c.serviceStartTime - c.arrivalTime;
      return wait > max ? wait : max;
    }, 0);

    return {
      totalServed: total,
      vipServed: vips.length,
      regularServed: regulars.length,
      avgWait: total > 0 ? (totalWait / total / 1000).toFixed(1) : "0.0",
      avgVipWait: vips.length > 0 ? (vipWait / vips.length / 1000).toFixed(1) : "0.0",
      avgRegWait: regulars.length > 0 ? (regWait / regulars.length / 1000).toFixed(1) : "0.0",
      maxWait: (maxWait / 1000).toFixed(1)
    };
  };

  const stats = calculateStats();

  return (
    <div className="app-container">
      {/* BACKGROUND NEON GLOW BLOBS */}
      <div className="bg-glow bg-glow-purple"></div>
      <div className="bg-glow bg-glow-blue"></div>

      {/* HEADER SECTION */}
      <header className="app-header">
        <div className="header-title-group">
          <h1>🏦 <span className="text-gradient">Banking Queue Simulator</span></h1>
          <p>Simulating Priority Queue & FIFO queue routing policies with starve prevention scheduling</p>
        </div>
        <div className="policy-badge">
          <span className="badge-dot" style={{ backgroundColor: autoSimEnabled ? '#10b981' : '#f59e0b' }}></span>
          <span>Simulation: <strong>{autoSimEnabled ? "RUNNING" : "PAUSED"}</strong></span>
        </div>
      </header>

      {/* DASHBOARD GRID */}
      <div className="dashboard-grid">
        
        {/* LEFT COLUMN: CONTROLS & FORM */}
        <aside className="control-sidebar">
          
          {/* Issue Token Form */}
          <div className="glass-panel">
            <h2 className="sidebar-title"><UserPlus size={20} className="text-gradient" /> Issue Token (Manual)</h2>
            <form onSubmit={handleFormSubmit}>
              <div className="form-group">
                <label>Customer Name</label>
                <input 
                  type="text" 
                  className="input-text" 
                  placeholder="e.g. Sumit Kumar"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  maxLength={25}
                />
              </div>

              <div className="form-group">
                <label>Customer Category</label>
                <select 
                  className="select-input"
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                >
                  <option value="REGULAR">Regular Customer (FIFO)</option>
                  <option value="VIP">VIP Customer (Priority Queue)</option>
                </select>
              </div>

              {formCategory === "VIP" && (
                <div className="form-group">
                  <label>VIP Priority Tier</label>
                  <select 
                    className="select-input"
                    value={formVipTier}
                    onChange={(e) => setFormVipTier(e.target.value)}
                  >
                    <option value="VVIP">VVIP (Level 1 - High)</option>
                    <option value="VIP">VIP (Level 2 - Medium)</option>
                    <option value="PREFERRED">Preferred (Level 3 - Low)</option>
                  </select>
                </div>
              )}

              <button type="submit" className="btn-primary" style={{ width: '100%', marginTop: '0.5rem' }}>
                Generate Token <ArrowRight size={16} />
              </button>
            </form>
          </div>

          {/* Starvation Prevention Controls */}
          <div className="glass-panel">
            <h2 className="sidebar-title"><ShieldAlert size={20} className="text-gradient" /> Starvation Policy</h2>
            
            <div className="toggle-wrapper">
              <span className="toggle-label">Prevention System</span>
              <label className="toggle-switch">
                <input 
                  type="checkbox"
                  checked={starvePreventionEnabled}
                  onChange={(e) => setStarvePreventionEnabled(e.target.checked)}
                />
                <span className="toggle-slider"></span>
              </label>
            </div>

            <div className="slider-group" style={{ opacity: starvePreventionEnabled ? 1 : 0.5 }}>
              <div className="slider-header">
                <span>Max Consecutive VIPs</span>
                <strong>{maxConsecutiveVips} VIPs</strong>
              </div>
              <input 
                type="range" 
                min="1" 
                max="8" 
                className="slider-input" 
                value={maxConsecutiveVips}
                onChange={(e) => setMaxConsecutiveVips(parseInt(e.target.value))}
                disabled={!starvePreventionEnabled}
              />
            </div>

            <div style={{ marginTop: '0.75rem', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              Consecutive VIPs served: <strong style={{ color: consecutiveVipsServed >= maxConsecutiveVips ? '#ef4444' : '#cbd5e1' }}>{consecutiveVipsServed} / {maxConsecutiveVips}</strong>
            </div>
          </div>

          {/* Simulation Settings */}
          <div className="glass-panel">
            <h2 className="sidebar-title"><Sliders size={20} className="text-gradient" /> Simulation Engine</h2>
            
            <div className="toggle-wrapper">
              <span className="toggle-label" style={{ fontWeight: 700 }}>Auto-Arrival & Serve</span>
              <label className="toggle-switch">
                <input 
                  type="checkbox"
                  checked={autoSimEnabled}
                  onChange={(e) => setAutoSimEnabled(e.target.checked)}
                />
                <span className="toggle-slider"></span>
              </label>
            </div>

            <div className="slider-group">
              <div className="slider-header">
                <span>Arrival Interval</span>
                <strong>{arrivalRate}s</strong>
              </div>
              <input 
                type="range" 
                min="1" 
                max="8" 
                className="slider-input"
                value={arrivalRate}
                onChange={(e) => setArrivalRate(parseInt(e.target.value))}
              />
            </div>

            <div className="slider-group">
              <div className="slider-header">
                <span>Service Duration</span>
                <strong>{minServiceTime}s - {maxServiceTime}s</strong>
              </div>
              <input 
                type="range" 
                min="2" 
                max="10" 
                className="slider-input"
                value={maxServiceTime}
                onChange={(e) => {
                  const val = parseInt(e.target.value);
                  setMaxServiceTime(val);
                  if (minServiceTime > val) setMinServiceTime(val);
                }}
              />
            </div>

            <div className="slider-group">
              <div className="slider-header">
                <span>VIP Arrival Rate</span>
                <strong>{vipProbability}%</strong>
              </div>
              <input 
                type="range" 
                min="10" 
                max="70" 
                className="slider-input"
                value={vipProbability}
                onChange={(e) => setVipProbability(parseInt(e.target.value))}
              />
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
              <button onClick={populateTestQueues} className="btn-secondary" style={{ flex: 1 }}>
                Load Demo Nodes
              </button>
              <button onClick={resetSimulator} className="btn-secondary" style={{ color: '#f87171', borderColor: 'rgba(239, 68, 68, 0.2)' }}>
                Reset All
              </button>
            </div>
          </div>
        </aside>

        {/* RIGHT COLUMN: QUEUES, COUNTERS & LOGS */}
        <main className="main-dashboard">
          
          {/* STATS PANEL ROW */}
          <div className="stat-panel-row">
            <div className="stat-card">
              <span className="stat-label">Served (VIP / REG)</span>
              <span className="stat-value">{stats.totalServed} <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>({stats.vipServed} / {stats.regularServed})</span></span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Avg. Wait Time</span>
              <span className="stat-value" style={{ color: '#6366f1' }}>{stats.avgWait}s</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Avg. VIP Wait</span>
              <span className="stat-value" style={{ color: '#c084fc' }}>{stats.avgVipWait}s</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Max Wait Time</span>
              <span className="stat-value" style={{ color: '#ef4444' }}>{stats.maxWait}s</span>
            </div>
          </div>

          {/* TELLER COUNTERS */}
          <section className="tellers-section">
            <h3 className="section-header"><Activity size={18} className="text-gradient" /> Service Teller Desks</h3>
            <div className="tellers-grid">
              {tellers.map(t => {
                const pct = t.busy ? ((t.serviceDuration - t.serviceTimeRemaining) / t.serviceDuration) * 100 : 0;
                
                return (
                  <div key={t.id} className={`teller-card ${t.busy ? 'busy' : ''}`}>
                    <div className="teller-header">
                      <div className="teller-info">
                        <div className="teller-avatar">{t.id}</div>
                        <span className="teller-name">{t.name}</span>
                      </div>
                      <span className={`teller-status-badge ${t.busy ? 'busy' : 'idle'}`}>
                        {t.busy ? 'Serving' : 'Idle'}
                      </span>
                    </div>

                    <div className="teller-customer">
                      {t.busy && t.currentCustomer ? (
                        <>
                          <span className="serving-label">Serving</span>
                          <div className="customer-details">
                            <div className="customer-name-token">
                              <span className="customer-name">{t.currentCustomer.name}</span>
                              <span className={`customer-token ${t.currentCustomer.type}`}>
                                {t.currentCustomer.tokenNumber}
                              </span>
                            </div>
                            <span className={`customer-badge ${t.currentCustomer.vipTier || 'REGULAR'}`}>
                              {t.currentCustomer.vipTier || 'Regular'}
                            </span>
                          </div>
                        </>
                      ) : (
                        <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem', fontStyle: 'italic' }}>
                          Awaiting customers...
                        </div>
                      )}
                    </div>

                    {/* Progress Bar */}
                    <div className="service-progress-container">
                      <div 
                        className="service-progress-bar" 
                        style={{ width: `${pct}%` }}
                      ></div>
                    </div>

                    <div className="teller-actions">
                      <button 
                        onClick={() => serveNextForTeller(t.id)} 
                        className="btn-secondary"
                        disabled={t.busy || (vipQueue.length === 0 && regularQueue.length === 0)}
                      >
                        Serve Next
                      </button>
                      <button 
                        onClick={() => completeTellerService(t.id)} 
                        className="btn-secondary"
                        disabled={!t.busy}
                        style={{ color: t.busy ? '#34d399' : '' }}
                      >
                        Complete
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </section>

          {/* QUEUES VISUALIZATION */}
          <section className="queues-container">
            
            {/* VIP Priority Queue */}
            <div className="queue-column">
              <div className="queue-column-header">
                <div className="queue-title-group">
                  <Star size={16} style={{ color: '#c084fc' }} fill="#c084fc" />
                  <h3 style={{ fontSize: '1rem', fontWeight: 700 }}>VIP Priority Queue</h3>
                </div>
                <span className="queue-count-badge vip">{vipQueue.length} waiting</span>
              </div>

              <div className="queue-cards-list">
                {vipQueue.length === 0 ? (
                  <div className="empty-queue-placeholder">
                    <Users size={28} />
                    <span>No VIP customers waiting</span>
                  </div>
                ) : (
                  vipQueue.map((c) => (
                    <div key={c.id} className={`customer-queue-card ${c.vipTier}`}>
                      <div className="card-left-info">
                        <span className="card-token-number">{c.tokenNumber}</span>
                        <span className="card-customer-name">{c.name}</span>
                      </div>
                      <div className="card-right-info">
                        <span className="card-wait-badge">{((Date.now() - c.arrivalTime) / 1000).toFixed(0)}s</span>
                        <span className="card-priority-num">Lvl {c.priorityLevel}</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Regular Queue */}
            <div className="queue-column">
              <div className="queue-column-header">
                <div className="queue-title-group">
                  <Users size={16} style={{ color: '#94a3b8' }} />
                  <h3 style={{ fontSize: '1rem', fontWeight: 700 }}>Regular Queue (FIFO)</h3>
                </div>
                <span className="queue-count-badge regular">{regularQueue.length} waiting</span>
              </div>

              <div className="queue-cards-list">
                {regularQueue.length === 0 ? (
                  <div className="empty-queue-placeholder">
                    <Users size={28} />
                    <span>No regular customers waiting</span>
                  </div>
                ) : (
                  regularQueue.map((c) => (
                    <div key={c.id} className="customer-queue-card REGULAR">
                      <div className="card-left-info">
                        <span className="card-token-number">{c.tokenNumber}</span>
                        <span className="card-customer-name">{c.name}</span>
                      </div>
                      <div className="card-right-info">
                        <span className="card-wait-badge">{((Date.now() - c.arrivalTime) / 1000).toFixed(0)}s</span>
                        <span className="card-priority-num">FIFO</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

          </section>

          {/* LIVE LOGS PANEL */}
          <section className="console-section">
            <div className="console-header">
              <div className="console-title">
                <Terminal size={16} />
                <span>Live Event Logs & Policy Audit Trails</span>
              </div>
              <div className="console-actions">
                <span className="console-badge-red"></span>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Console Output</span>
              </div>
            </div>
            
            <div className="console-logs" ref={consoleLogsRef}>
              {logs.length === 0 ? (
                <div style={{ color: 'var(--text-muted)', fontStyle: 'italic', padding: '0.5rem 0' }}>
                  No system logs. Issue tokens or start auto-simulation to view events...
                </div>
              ) : (
                logs.map((log, idx) => (
                  <div key={idx} className={`console-line ${log.type}`}>
                    <span className="timestamp">[{log.timestamp}]</span>
                    <span>{log.message}</span>
                  </div>
                ))
              )}
            </div>
          </section>

          {/* AUDIT HISTORY TABLE */}
          {servedCustomers.length > 0 && (
            <section className="history-section">
              <h3 className="section-header"><BarChart3 size={18} className="text-gradient" /> Completed Service Audit Ledger (Last 10)</h3>
              <div className="history-table-wrapper">
                <table className="history-table">
                  <thead>
                    <tr>
                      <th>Token</th>
                      <th>Customer Name</th>
                      <th>Category</th>
                      <th>Priority Tier</th>
                      <th>Wait Duration</th>
                      <th>Service Duration</th>
                    </tr>
                  </thead>
                  <tbody>
                    {servedCustomers.slice(-10).reverse().map((c) => (
                      <tr key={c.id}>
                        <td style={{ fontFamily: 'var(--font-mono)', fontWeight: 700 }}>{c.tokenNumber}</td>
                        <td style={{ fontWeight: 500 }}>{c.name}</td>
                        <td>
                          <span style={{ 
                            fontSize: '0.7rem', 
                            padding: '0.15rem 0.35rem', 
                            borderRadius: '4px',
                            background: c.type === 'VIP' ? 'rgba(192, 132, 252, 0.15)' : 'rgba(148, 163, 184, 0.15)',
                            color: c.type === 'VIP' ? '#c084fc' : '#cbd5e1'
                          }}>
                            {c.type}
                          </span>
                        </td>
                        <td>{c.vipTier || "Regular (N/A)"}</td>
                        <td style={{ fontFamily: 'var(--font-mono)' }}>{((c.serviceStartTime - c.arrivalTime) / 1000).toFixed(1)}s</td>
                        <td style={{ fontFamily: 'var(--font-mono)' }}>{(c.serviceDuration / 1000).toFixed(1)}s</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

        </main>
      </div>
    </div>
  );
}

export default App;
