import "@testing-library/jest-dom";
import { afterEach, vi } from "vitest";
import { cleanup } from "@testing-library/react";

// Automatically clean up rendering context after each test case
afterEach(() => {
  cleanup();
});

// Mock browser scrollIntoView
window.HTMLElement.prototype.scrollIntoView = vi.fn();
