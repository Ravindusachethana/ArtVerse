import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  ViewChild
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

// chart.js v4 is tree-shakeable; register the pieces we use once, up front.
Chart.register(...registerables);

/**
 * Thin standalone wrapper around a Chart.js canvas so report pages can bind a
 * reactive [config] signal and let Angular handle create / update / teardown.
 * Avoids pulling in a heavier charting dependency (see admin README).
 */
@Component({
  selector: 'app-chart',
  standalone: true,
  template: '<canvas #canvas></canvas>',
  styles: [':host { display: block; position: relative; width: 100%; height: 100%; }']
})
export class ChartComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input({ required: true }) config!: ChartConfiguration;

  @ViewChild('canvas', { static: true }) private canvasRef!: ElementRef<HTMLCanvasElement>;

  private chart?: Chart;

  ngAfterViewInit(): void {
    this.render();
  }

  ngOnChanges(): void {
    // Fires on every [config] change; rebuild so data and options stay in sync.
    if (this.canvasRef) this.render();
  }

  private render(): void {
    this.chart?.destroy();
    this.chart = new Chart(this.canvasRef.nativeElement, this.config);
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
